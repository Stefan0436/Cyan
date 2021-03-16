package org.asf.cyan.mixin.services;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.asf.cyan.api.events.core.ITypedSynchronizedEventListener;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.util.IConsumer;

public class CyanMixinPhaseHandler implements ITypedSynchronizedEventListener<LoadPhase> {

	@Override
	public String getListenerName() {
		return "Cyan Mixin Phase Listener";
	}

	@Override
	public void received(LoadPhase cyanPhase) {
		IConsumer<Phase> consumer = (phase) -> {
			try {
				Method meth = MixinEnvironment.class.getDeclaredMethod("gotoPhase", Phase.class);
				meth.setAccessible(true);
				meth.invoke(null, phase);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		};
		switch (cyanPhase) {
		case NOT_READY:
		case CORELOAD:
		case PRELOAD:
			consumer.accept(Phase.PREINIT);
		case INIT:
		case POSTINIT:
			consumer.accept(Phase.INIT);
		case RUNTIME:
			consumer.accept(Phase.DEFAULT);
		}
	}
	
}
