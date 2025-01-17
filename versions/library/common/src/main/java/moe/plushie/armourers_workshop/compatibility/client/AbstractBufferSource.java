package moe.plushie.armourers_workshop.compatibility.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import moe.plushie.armourers_workshop.api.client.IBufferSource;
import moe.plushie.armourers_workshop.api.client.IVertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class AbstractBufferSource implements IBufferSource {

    private static final Cache<Object, AbstractBufferSource> CACHED_BUFFER_SOURCES = CacheBuilder.newBuilder()
            .expireAfterAccess(3, TimeUnit.SECONDS)
            //.removalListener(AbstractBufferSource::release)
            .build();

    private final HashMap<VertexConsumer, IVertexConsumer> cachedBuffers = new HashMap<>();
    private final MultiBufferSource bufferSource;

    public AbstractBufferSource(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    public static IBufferSource defaultBufferSource() {
        return wrap(Minecraft.getInstance().renderBuffers().bufferSource());
    }

    public static IBufferSource immediateBufferSource(BufferBuilder bufferBuilder) {
        return new AbstractBufferSource(MultiBufferSource.immediate(bufferBuilder));
    }

    public static IBufferSource wrap(MultiBufferSource bufferSource) {
        AbstractBufferSource bufferSource1 = CACHED_BUFFER_SOURCES.getIfPresent(bufferSource);
        if (bufferSource1 == null) {
            bufferSource1 = new AbstractBufferSource(bufferSource);
            CACHED_BUFFER_SOURCES.put(bufferSource, bufferSource1);
        }
        return bufferSource1;
    }

    public static MultiBufferSource unwrap(IBufferSource bufferSource) {
        return ((AbstractBufferSource) bufferSource).bufferSource;
    }

    @Override
    public IVertexConsumer getBuffer(RenderType renderType) {
        VertexConsumer builder = bufferSource.getBuffer(renderType);
        return cachedBuffers.computeIfAbsent(builder, AbstractVertexConsumer::new);
    }

    @Override
    public void endBatch() {
        if (bufferSource instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) bufferSource).endBatch();
        }
    }
}
