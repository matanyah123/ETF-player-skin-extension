package net.matanyah.pse;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ETFTextureVariantResolver {
	private static final Object NO_PROVIDER = new Object();
	private static final Map<Identifier, Object> VARIANT_PROVIDER_CACHE = new ConcurrentHashMap<>();
	private static final Set<String> DEBUG_LOG_KEYS = ConcurrentHashMap.newKeySet();
	private static final Pattern DOT_SUFFIX_PATTERN = Pattern.compile("^(.*)\\.(\\d+)\\.([^.]+)$");
	private static final Pattern PLAIN_SUFFIX_PATTERN = Pattern.compile("^(.*\\D)(\\d+)\\.([^.]+)$");

	private static volatile Method stateFactory;
	private static volatile Method variantProviderFactory;
	private static volatile Method propertyRuleFactory;
	private static volatile Method ruleAlwaysMetMethod;
	private static volatile Method ruleMatchesMethod;
	private static volatile Method ruleVariantResolver;
	private static volatile Method suffixResolver;
	private static volatile Field ruleNumberField;
	private static volatile boolean initialized;
	private static volatile boolean loggedFailure;

	private ETFTextureVariantResolver() {}

	public static void clearCache() {
		VARIANT_PROVIDER_CACHE.clear();
		DEBUG_LOG_KEYS.clear();
	}

	public static Identifier resolve(Identifier defaultTexture, Entity entity) {
		return resolveDetailed(defaultTexture, entity).selectedTexture();
	}

	public static Resolution resolveDetailed(Identifier texture, Entity entity) {
		return resolveDetailed(texture, (Object) entity);
	}

	public static Resolution resolveDetailed(Identifier texture, Object entityOrState) {
		if (!initializeReflection()) {
			return new Resolution(texture, texture, 1, false, DynamicSkinTextureRegistry.VariantFlags.NONE);
		}

		try {
			Identifier baseTexture = normalizeToBaseTexture(texture);
			Object variantProvider = getVariantProvider(baseTexture);
			if (variantProvider == null) {
				return new Resolution(texture, baseTexture, 1, false, DynamicSkinTextureRegistry.VariantFlags.NONE);
			}

			Object entityState = stateFactory.invoke(null, entityOrState);
			if (variantProvider instanceof DynamicVariantProvider dynamicVariantProvider) {
				return dynamicVariantProvider.resolve(texture, baseTexture, entityState);
			}

			Object resolvedSuffix = suffixResolver.invoke(variantProvider, entityState);
			int resolvedVariantIndex = resolvedSuffix instanceof Integer value ? value : 1;
			DynamicSkinTextureRegistry.VariantFlags flags = DynamicSkinTextureRegistry.getVariantFlags(baseTexture, resolvedVariantIndex);
			if (!(resolvedSuffix instanceof Integer) || resolvedVariantIndex < 2) {
				return new Resolution(texture, baseTexture, 1, true, DynamicSkinTextureRegistry.getVariantFlags(baseTexture, 1));
			}

			return new Resolution(
					texture,
					baseTexture,
					resolvedVariantIndex,
					true,
					flags
			);
		} catch (ReflectiveOperationException | RuntimeException error) {
			if (!loggedFailure) {
				loggedFailure = true;
				PlayerSkinExtensionETF.LOGGER.debug("Could not resolve ETF texture variant reflectively", error);
			}
			return new Resolution(texture, texture, 1, false, DynamicSkinTextureRegistry.VariantFlags.NONE);
		}
	}

	public static void logResolution(Entity entity, Identifier requestedTexture, Resolution resolution, boolean inject) {
		Identifier selectedTexture = resolution.selectedTexture();
		String key = entity.getUUID() + "|" + requestedTexture + "|" + selectedTexture + "|" + resolution.variantIndex() + "|" + inject;
		if (!DEBUG_LOG_KEYS.add(key)) {
			return;
		}

		PlayerSkinExtensionETF.LOGGER.info(
				"[PSE] entity='{}' requested='{}' base='{}' selected='{}' variant={} providerFound={} dynamic={} player={} asset={} nameSource={} nameSlot={} inject={}",
				entity.getCustomName() == null ? entity.getName().getString() : entity.getCustomName().getString(),
				requestedTexture,
				resolution.baseTexture(),
				selectedTexture,
				resolution.variantIndex(),
				resolution.providerFound(),
				resolution.flags().dynamic(),
				resolution.flags().player(),
				resolution.flags().assetType() == null ? "none" : resolution.flags().assetType().name().toLowerCase(),
				resolution.flags().nameSource().propertyValue(),
				resolution.flags().nameSlot(),
				inject
		);
	}

	private static Object getVariantProvider(Identifier defaultTexture) throws ReflectiveOperationException {
		Object cached = VARIANT_PROVIDER_CACHE.get(defaultTexture);
		if (cached == NO_PROVIDER) {
			return null;
		}
		if (cached != null) {
			return cached;
		}

		Identifier propertiesId = Identifier.fromNamespaceAndPath(
				defaultTexture.getNamespace(),
				defaultTexture.getPath().replace(".png", ".properties")
		);
		// ETF itself only understands numbered suffix rules. Dynamic slots need a
		// custom evaluator so unconditional "dynamic" can behave as a default while
		// conditional dynamic rules still participate in matching.
		Object provider = getDynamicVariantProvider(propertiesId, defaultTexture);
		if (provider == null) {
			// Ask ETF for the provider bound to this specific override texture instead of
			// reusing the entity's base suffix, so submodel textures can have their own rules.
			provider = variantProviderFactory.invoke(null, propertiesId, defaultTexture, new String[]{"skins", "textures"});
		}
		VARIANT_PROVIDER_CACHE.put(defaultTexture, provider == null ? NO_PROVIDER : provider);
		return provider;
	}

	private static Object getDynamicVariantProvider(Identifier propertiesId, Identifier defaultTexture) throws ReflectiveOperationException {
		Properties props = loadProperties(propertiesId);
		if (props == null) {
			return null;
		}

		Map<Integer, DynamicSkinTextureRegistry.VariantFlags> dynamicRuleFlags = new HashMap<>();
		boolean hasDynamicRule = false;
		for (int i = 1; i <= 64; i++) {
			DynamicSkinTextureRegistry.VariantFlags flags = DynamicSkinTextureRegistry.getVariantFlags(props, i);
			boolean isDynamic = false;
			for (String key : List.of("skins", "textures")) {
				String propertyKey = key + "." + i;
				String value = props.getProperty(propertyKey);
				if (value != null && "dynamic".equalsIgnoreCase(value.trim())) {
					props.setProperty(propertyKey, Integer.toString(i));
					isDynamic = true;
					hasDynamicRule = true;
				}
			}
			if (isDynamic || flags.player()) {
				dynamicRuleFlags.put(i, flags);
			}
		}

		if (!hasDynamicRule) {
			return null;
		}

		List<?> parsedRules = (List<?>) propertyRuleFactory.invoke(null, props, propertiesId, new String[]{"skins", "textures"});
		if (parsedRules.isEmpty()) {
			return null;
		}

		List<DynamicRule> rules = new ArrayList<>(parsedRules.size());
		for (Object parsedRule : parsedRules) {
			int ruleNumber = ruleNumberField.getInt(parsedRule);
			DynamicSkinTextureRegistry.VariantFlags flags = dynamicRuleFlags.getOrDefault(
					ruleNumber,
					DynamicSkinTextureRegistry.getVariantFlags(defaultTexture, ruleNumber)
			);
			boolean isDefaultDynamic = flags.dynamic() && (boolean) ruleAlwaysMetMethod.invoke(parsedRule);
			rules.add(new DynamicRule(parsedRule, flags, isDefaultDynamic));
		}
		return new DynamicVariantProvider(rules);
	}

	private static Properties loadProperties(Identifier propertiesId) {
		return Minecraft.getInstance().getResourceManager()
				.getResource(propertiesId)
				.map(resource -> {
					try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
						Properties props = new Properties();
						props.load(reader);
						return props;
					} catch (IOException error) {
						return null;
					}
				})
				.orElse(null);
	}

	private static Identifier normalizeToBaseTexture(Identifier texture) {
		Object directProvider = VARIANT_PROVIDER_CACHE.get(texture);
		if (directProvider != null && directProvider != NO_PROVIDER) {
			return texture;
		}

		Identifier dotNormalized = stripVariantSuffix(texture, DOT_SUFFIX_PATTERN);
		if (dotNormalized != null) {
			return dotNormalized;
		}

		Identifier plainNormalized = stripVariantSuffix(texture, PLAIN_SUFFIX_PATTERN);
		return plainNormalized == null ? texture : plainNormalized;
	}

	private static Identifier stripVariantSuffix(Identifier texture, Pattern pattern) {
		Matcher matcher = pattern.matcher(texture.getPath());
		if (!matcher.matches()) {
			return null;
		}
		return Identifier.fromNamespaceAndPath(texture.getNamespace(), matcher.group(1) + "." + matcher.group(3));
	}

	private static boolean initializeReflection() {
		if (initialized) {
			return stateFactory != null
					&& variantProviderFactory != null
					&& propertyRuleFactory != null
					&& ruleAlwaysMetMethod != null
					&& ruleMatchesMethod != null
					&& ruleVariantResolver != null
					&& ruleNumberField != null
					&& suffixResolver != null;
		}

		synchronized (ETFTextureVariantResolver.class) {
			if (initialized) {
				return stateFactory != null
						&& variantProviderFactory != null
						&& propertyRuleFactory != null
						&& ruleAlwaysMetMethod != null
						&& ruleMatchesMethod != null
						&& ruleVariantResolver != null
						&& ruleNumberField != null
						&& suffixResolver != null;
			}

			initialized = true;
			try {
				Class<?> apiClass = Class.forName("traben.entity_texture_features.ETFApi");
				stateFactory = apiClass.getMethod("stateOfEntityOrEntityState", Object.class);
				variantProviderFactory = apiClass.getMethod("getVariantSupplierOrNull", Identifier.class, Identifier.class, String[].class);

				Class<?> providerClass = Class.forName("traben.entity_texture_features.ETFApi$ETFVariantSuffixProvider");
				Class<?> propertiesProviderClass = Class.forName("traben.entity_texture_features.features.property_reading.PropertiesRandomProvider");
				Class<?> ruleClass = Class.forName("traben.entity_texture_features.features.property_reading.RandomPropertyRule");
				propertyRuleFactory = propertiesProviderClass.getMethod("getAllValidPropertyObjects", Properties.class, Identifier.class, String[].class);
				ruleAlwaysMetMethod = ruleClass.getMethod("isAlwaysMet");
				ruleMatchesMethod = findRuleMethod(ruleClass, "doesEntityMeetConditionsOfThisCase", boolean.class, 3);
				ruleVariantResolver = findRuleMethod(ruleClass, "getVariantSuffixFromThisCase", int.class, 1);
				ruleNumberField = ruleClass.getField("ruleNumber");

				for (Method method : providerClass.getMethods()) {
					if (Modifier.isStatic(method.getModifiers())) continue;
					if (!method.getName().equals("getSuffixForETFEntity")) continue;
					if (method.getParameterCount() != 1) continue;
					if (method.getReturnType() != int.class) continue;

					suffixResolver = method;
					break;
				}
			} catch (ClassNotFoundException ignored) {}
			catch (NoSuchMethodException ignored) {}
			catch (NoSuchFieldException ignored) {}

			return stateFactory != null
					&& variantProviderFactory != null
					&& propertyRuleFactory != null
					&& ruleAlwaysMetMethod != null
					&& ruleMatchesMethod != null
					&& ruleVariantResolver != null
					&& ruleNumberField != null
					&& suffixResolver != null;
		}
	}

	private static Method findRuleMethod(Class<?> ruleClass, String methodName, Class<?> returnType, int parameterCount) {
		for (Method method : ruleClass.getMethods()) {
			if (!method.getName().equals(methodName)) continue;
			if (method.getParameterCount() != parameterCount) continue;
			if (method.getReturnType() != returnType) continue;
			return method;
		}
		return null;
	}

	private static int resolveVariantFromRule(Object rule, Object entityState) throws ReflectiveOperationException {
		Object resolvedVariant = ruleVariantResolver.invoke(rule, entityState);
		return resolvedVariant instanceof Integer value ? value : 1;
	}

	private record DynamicRule(Object rule, DynamicSkinTextureRegistry.VariantFlags flags, boolean defaultDynamic) {}

	private static final class DynamicVariantProvider {
		private final List<DynamicRule> rules;

		private DynamicVariantProvider(List<DynamicRule> rules) {
			this.rules = List.copyOf(rules);
		}

		private Resolution resolve(Identifier requestedTexture, Identifier baseTexture, Object entityState) throws ReflectiveOperationException {
			DynamicRule deferredDefaultDynamic = null;

			for (DynamicRule rule : rules) {
				if (rule.defaultDynamic()) {
					if (deferredDefaultDynamic == null) {
						deferredDefaultDynamic = rule;
					}
					continue;
				}

				boolean matches = (boolean) ruleMatchesMethod.invoke(rule.rule(), entityState, false, null);
				if (matches) {
					return new Resolution(
							requestedTexture,
							baseTexture,
							resolveVariantFromRule(rule.rule(), entityState),
							true,
							rule.flags()
					);
				}
			}

			if (deferredDefaultDynamic != null) {
				return new Resolution(
						requestedTexture,
						baseTexture,
						resolveVariantFromRule(deferredDefaultDynamic.rule(), entityState),
						true,
						deferredDefaultDynamic.flags()
				);
			}

			return new Resolution(requestedTexture, baseTexture, 1, true, DynamicSkinTextureRegistry.VariantFlags.NONE);
		}
	}

	public record Resolution(Identifier requestedTexture, Identifier baseTexture, int variantIndex, boolean providerFound, DynamicSkinTextureRegistry.VariantFlags flags) {
		public Identifier selectedTexture() {
			return flags.dynamic() ? baseTexture : DynamicSkinTextureRegistry.getTextureForVariant(baseTexture, variantIndex);
		}
	}
}
