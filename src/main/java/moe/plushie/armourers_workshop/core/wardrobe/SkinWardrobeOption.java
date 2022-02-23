package moe.plushie.armourers_workshop.core.wardrobe;

import moe.plushie.armourers_workshop.core.entity.MannequinEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.IDataSerializer;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum SkinWardrobeOption {

    ARMOUR_HEAD(EquipmentSlotType.HEAD),
    ARMOUR_CHEST(EquipmentSlotType.CHEST),
    ARMOUR_LEGS(EquipmentSlotType.LEGS),
    ARMOUR_FEET(EquipmentSlotType.FEET),

    MANNEQUIN_IS_CHILD(MannequinEntity.DATA_IS_CHILD),
    MANNEQUIN_IS_FLYING(MannequinEntity.DATA_IS_FLYING),
    MANNEQUIN_IS_VISIBLE(MannequinEntity.DATA_IS_VISIBLE),
    MANNEQUIN_IS_GHOST(MannequinEntity.DATA_IS_GHOST),
    MANNEQUIN_EXTRA_RENDER(MannequinEntity.DATA_EXTRA_RENDERER);

    private final boolean broadcastChanges;
    private final DataAccessor<?> dataAccessor;

    SkinWardrobeOption(EquipmentSlotType slotType) {
        this.broadcastChanges = true;
        this.dataAccessor = DataAccessor.withDataSerializer(DataSerializers.BOOLEAN)
                .withSupplier((wardrobe) -> wardrobe.shouldRenderEquipment(slotType))
                .withApplier((wardrobe, value) -> wardrobe.setRenderEquipment(slotType, value));
    }

    <T> SkinWardrobeOption(DataParameter<T> dataParameter) {
        this.broadcastChanges = false;
        this.dataAccessor = DataAccessor.withDataSerializer(dataParameter.getSerializer())
                .withSupplier((wardrobe) -> entityData(wardrobe).map(data -> data.get(dataParameter)).orElse(null))
                .withApplier(((wardrobe, value) -> entityData(wardrobe).ifPresent(data -> data.set(dataParameter, value))));
    }

    private static Optional<EntityDataManager> entityData(SkinWardrobe wardrobe) {
        Entity entity = wardrobe.getEntity();
        if (entity != null) {
            return Optional.of(entity.getEntityData());
        }
        return Optional.empty();
    }

    public <T> void set(SkinWardrobe wardrobe, T value) {
        DataAccessor<T> dataAccessor = getDataAccessor();
        if (dataAccessor.applier != null) {
            dataAccessor.applier.accept(wardrobe, value);
        }
    }

    public <T> T get(SkinWardrobe wardrobe, T defaultValue) {
        DataAccessor<T> dataAccessor = getDataAccessor();
        if (dataAccessor.supplier != null) {
            T value = dataAccessor.supplier.apply(wardrobe);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> DataAccessor<T> getDataAccessor() {
        return (DataAccessor<T>) dataAccessor;
    }

    public <T> IDataSerializer<T> getDataSerializer() {
        DataAccessor<T> dataAccessor = getDataAccessor();
        return dataAccessor.dataSerializer;
    }

    public boolean isBroadcastChanges() {
        return broadcastChanges;
    }

    public static class DataAccessor<T> {
        IDataSerializer<T> dataSerializer;
        Function<SkinWardrobe, T> supplier;
        BiConsumer<SkinWardrobe, T> applier;

        public static <T> DataAccessor<T> withDataSerializer(IDataSerializer<T> dataSerializer) {
            DataAccessor<T> dataAccessor = new DataAccessor<>();
            dataAccessor.dataSerializer = dataSerializer;
            return dataAccessor;
        }

        public DataAccessor<T> withApplier(BiConsumer<SkinWardrobe, T> applier) {
            this.applier = applier;
            return this;
        }

        public DataAccessor<T> withSupplier(Function<SkinWardrobe, T> supplier) {
            this.supplier = supplier;
            return this;
        }
    }
}