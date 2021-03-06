package si.meansoft.traincraft;

import net.darkhax.tesla.api.ITeslaConsumer;
import net.darkhax.tesla.api.ITeslaHolder;
import net.darkhax.tesla.api.ITeslaProducer;
import net.darkhax.tesla.capability.TeslaCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.EnergyStorage;

/**
 * @author canitzp
 */
public class TeslaWrapper implements ITeslaHolder, ITeslaConsumer, ITeslaProducer{

    private EnergyStorage storage;

    private TeslaWrapper(EnergyStorage storage){
        this.storage = storage;
    }

    public static <T> T getFromCapability(EnergyStorage storage, Capability<T> c){
        if(c == TeslaCapabilities.CAPABILITY_HOLDER || c == TeslaCapabilities.CAPABILITY_HOLDER || c == TeslaCapabilities.CAPABILITY_PRODUCER){
            return (T) new TeslaWrapper(storage);
        }
        return null;
    }

    @Override
    public long getStoredPower(){
        return this.storage.getEnergyStored();
    }

    @Override
    public long getCapacity(){
        return this.storage.getMaxEnergyStored();
    }

    @Override
    public long givePower(long power, boolean simulated){
        return this.storage.receiveEnergy((int) power, simulated);
    }

    @Override
    public long takePower(long power, boolean simulated){
        return this.storage.extractEnergy((int) power, simulated);
    }
}
