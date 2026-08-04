package pers.solid.mishang.uc.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: This mixin references custom Fabric events (BEGIN_ATTACK_BLOCK_EVENT, PROGRESS_ATTACK_BLOCK_EVENT)
// which have been removed during Forge conversion. Need to redesign using Forge events
// (PlayerInteractEvent.LeftClickBlock) or recreate custom Forge events.
// For now, this mixin is disabled - its class name is removed from mishanguc.mixins.json.

/**
 * This mixin is temporarily disabled during Fabric→Forge conversion.
 * The original purpose was to intercept block attack events at the minecraft level,
 * distinguishing between "begin attack" and "progress attack" phases.
 * On Forge, this can potentially be handled via PlayerInteractEvent.LeftClickBlock.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class BetterClientPlayerInteractionManagerMixin {

  @Shadow
  @Final
  private Minecraft minecraft;
  @Shadow
  private GameType localPlayerMode;

  @Shadow
  protected abstract void startPrediction(ClientLevel level, PredictiveAction action);

  // TODO: Re-implement with Forge event system
  // Original: intercepted startDestroyBlock and continueDestroyBlock to dispatch custom events
}
