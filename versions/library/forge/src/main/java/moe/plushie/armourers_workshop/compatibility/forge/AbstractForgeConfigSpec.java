package moe.plushie.armourers_workshop.compatibility.forge;

import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.api.config.IConfigBuilder;
import moe.plushie.armourers_workshop.api.config.IConfigSpec;
import moe.plushie.armourers_workshop.compatibility.AbstractConfigSpec;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Available("[1.21, )")
public class AbstractForgeConfigSpec extends AbstractConfigSpec {

    public AbstractForgeConfigSpec(Type type, HashMap<String, Value<Object>> values) {
        super(type, values);
    }

    public static <B extends IConfigBuilder> IConfigSpec create(Type type, Function<IConfigBuilder, B> applier) {
        // create a builder from loader.
        Pair<B, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(builder -> applier.apply(new Builder() {

            @Override
            public IConfigSpec build() {
                return new AbstractForgeConfigSpec(type, values);
            }

            @Override
            protected Builder push(String name) {
                builder.push(name);
                return this;
            }

            @Override
            protected Builder pop() {
                builder.pop();
                return this;
            }

            @Override
            protected Builder comment(String... comment) {
                builder.comment(comment);
                return this;
            }

            @Override
            protected Value<Boolean> define(String path, boolean defaultValue) {
                return cast(path, builder.define(path, defaultValue));
            }

            @Override
            protected Value<Integer> defineInRange(String path, int defaultValue, int minValue, int maxValue) {
                return cast(path, builder.defineInRange(path, defaultValue, minValue, maxValue));
            }

            @Override
            protected Value<Double> defineInRange(String path, double defaultValue, double minValue, double maxValue) {
                return cast(path, builder.defineInRange(path, defaultValue, minValue, maxValue));
            }

            @Override
            protected <T> Value<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
                return cast(path, builder.defineList(path, defaultValue, elementValidator));
            }

            <T> Value<T> cast(String path, ModConfigSpec.ConfigValue<T> value) {
                return new Value<>(path, value, value::set);
            }
        }));

        // bind the config to spec.
        AbstractForgeConfigSpec spec = (AbstractForgeConfigSpec) pair.getLeft().build();
        spec.bind(pair.getRight(), ModConfigSpec::save);

        // registry the config into loader.
        ModConfigSpec config = pair.getRight();
        ModLoadingContext.get().registerConfig(ModConfig.Type.valueOf(type.name()), config);

        return spec;
    }
}
