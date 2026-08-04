package pers.solid.mishang.uc.data.stubs;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public interface ClientCommandRegistrationCallback {
    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess);

    Event EVENT = new Event();

    class Event {
        public void register(ClientCommandRegistrationCallback callback) {}
    }
}
