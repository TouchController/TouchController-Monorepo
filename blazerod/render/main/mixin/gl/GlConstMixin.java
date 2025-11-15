package top.fifthlight.blazerod.mixin.gl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.TextureFormat;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.blazerod.extension.AddressModeExt;
import top.fifthlight.blazerod.extension.ShaderTypeExt;
import top.fifthlight.blazerod.extension.TextureFormatExt;

@Mixin(GlConst.class)
public class GlConstMixin {
    @Inject(method = "toGlInternalId(Lcom/mojang/blaze3d/textures/TextureFormat;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlInternalId(TextureFormat textureFormat, CallbackInfoReturnable<Integer> cir) {
        if (textureFormat == TextureFormatExt.RGBA32F) {
            cir.setReturnValue(GL30C.GL_RGBA32F);
        } else if (textureFormat == TextureFormatExt.RGB32F) {
            cir.setReturnValue(GL30C.GL_RGB32F);
        } else if (textureFormat == TextureFormatExt.RG32F) {
            cir.setReturnValue(GL30C.GL_RG32F);
        } else if (textureFormat == TextureFormatExt.R32F) {
            cir.setReturnValue(GL30C.GL_R32F);
        } else if (textureFormat == TextureFormatExt.R32I) {
            cir.setReturnValue(GL30C.GL_R32I);
        }
    }

    @Inject(method = "toGlExternalId(Lcom/mojang/blaze3d/textures/TextureFormat;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlExternalId(TextureFormat textureFormat, CallbackInfoReturnable<Integer> cir) {
        if (textureFormat == TextureFormatExt.RGBA32F) {
            cir.setReturnValue(GL11C.GL_RGBA);
        } else if (textureFormat == TextureFormatExt.RGB32F) {
            cir.setReturnValue(GL11C.GL_RGB);
        } else if (textureFormat == TextureFormatExt.RG32F) {
            cir.setReturnValue(GL30C.GL_RG);
        } else if (textureFormat == TextureFormatExt.R32F) {
            cir.setReturnValue(GL11C.GL_RED);
        } else if (textureFormat == TextureFormatExt.R32I) {
            cir.setReturnValue(GL30C.GL_RED);
        }
    }

    @Inject(method = "toGlType(Lcom/mojang/blaze3d/textures/TextureFormat;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlTextureFormat(TextureFormat textureFormat, CallbackInfoReturnable<Integer> cir) {
        if (textureFormat == TextureFormatExt.RGBA32F) {
            cir.setReturnValue(GL11C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.RGB32F) {
            cir.setReturnValue(GL11C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.RG32F) {
            cir.setReturnValue(GL30C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.R32F) {
            cir.setReturnValue(GL30C.GL_FLOAT);
        } else if (textureFormat == TextureFormatExt.R32I) {
            cir.setReturnValue(GL30C.GL_INT);
        }
    }

    @Inject(method = "toGl(Lcom/mojang/blaze3d/textures/AddressMode;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlAddressMode(AddressMode addressMode, CallbackInfoReturnable<Integer> cir) {
        if (addressMode == AddressModeExt.MIRRORED_REPEAT) {
            cir.setReturnValue(GL30C.GL_MIRRORED_REPEAT);
        }
    }

    @Inject(method = "toGl(Lcom/mojang/blaze3d/shaders/ShaderType;)I", at = @At("HEAD"), cancellable = true)
    private static void onToGlShaderType(ShaderType shaderType, CallbackInfoReturnable<Integer> cir) {
        if (shaderType == ShaderTypeExt.COMPUTE) {
            cir.setReturnValue(GL43C.GL_COMPUTE_SHADER);
        }
    }
}
