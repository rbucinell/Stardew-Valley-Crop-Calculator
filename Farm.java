import java.util.ArrayList;

public class Farm implements Comparable<Farm>
{
    private static ArrayList<Crop> cropTypes; //all unique types of crops
    private static int daysRemaining;
    private ArrayList<CropGroup> crops; //the crops currently on the farm
    private int gold;
    private int goldCache; //crops are not sold immediately after harvesting; the gold is obtained the following day
    private ArrayList<FarmEvent> events; //a snapshot of the events that occurred on this farm every day
    private FarmEvent event; //the events for the current day on this farm

    public Farm(ArrayList<CropGroup> crops, int gold, int goldCache, ArrayList<FarmEvent> events)
    {
        this.gold = gold;
        this.goldCache = goldCache;
        this.event = null;

        this.crops = new ArrayList<>();

        if (crops != null)
        {
            for (CropGroup cropGroup : crops)
            {
                this.crops.add(cropGroup.clone());
            }
        }

        this.events = new ArrayList<>();

        if (events != null)
        {
            for (FarmEvent farmEvent : events)
            {
                this.events.add(farmEvent.clone());
            }
        }
    }

    /**
     * Performs first time initilization for all farms.
     *
     * All farms share the exact same types of crops
     * they can choose from to plant on the farm and
     * the exact same number of days remaining before
     * the end of the season, so these are static
     * variables.
     */
    public static void initialize(ArrayList<Crop> crops, int daysLeft)
    {
        cropTypes = new ArrayList<>(crops);
        daysRemaining = daysLeft;
    }

    /**
     * Updates all farms for the beginning of a new day.
     *
     * Filters out and removes all seeds that could not
     * possibly yield crops before the end of the season.
     *
     * Decrements the number of days remaining.
     */
    public static void update()
    {
        daysRemaining--;

        ArrayList<Crop> invalidCrops = new ArrayList<>();
        for (Crop cropType : cropTypes)
        {
            if (cropType.getGrowthTime() > daysRemaining)
            {
                invalidCrops.add(cropType);
            }
        }
        cropTypes.removeAll(invalidCrops);
    }

    /**
     * Main logic loop for a farm.
     *
     * Advances the crops, harvests the crops, and invests in more crops.
     *
     * @return Every single valid permutation of this farm possible, advanced one day.
     */
    ArrayList<Farm> simulateDay()
    {
        advanceCrops();
        gold += goldCache;
        goldCache = 0;

        event = new FarmEvent(daysRemaining, gold, crops);
        events.add(event);

        harvest();

        //there are no days remaining before the end of the season
        //so pretend the crops you sold today give you instant gold
        if (daysRemaining == 0)
        {
            gold += goldCache;
        }

        ArrayList<Farm> farmPermutations = invest();
        return farmPermutations;
    }

    private void advanceCrops()
    {
        for (CropGroup cropGroup : crops)
        {
            cropGroup.advance();
        }
    }

    /**
     * Harvest the crops that are ready for harvesting and adds their values to the gold cache.
     *
     * Additionally removes crops from the farm if they meet either of the following conditions:
     *      They have been harvested and cannot regrow
     *      They cannot grow to maturity (or regrow) before the end of the season
     *
     * Because of the above conditions, do not be alarmed if the crops growing on your farm
     * disappear in the logs after a certain day. It just means they would not have yielded
     * fruit before the end of the season, and the program cleaned them up to improve runtime.
     */
    private void harvest()
    {
        //maintain a list of all the crops to remove
        ArrayList<CropGroup> worthlessCrops = new ArrayList<>();
        for (CropGroup cropGroup : crops)
        {
            int amount = cropGroup.harvest(event);
            goldCache += amount;

            if (!cropGroup.canProduceMore(daysRemaining))
            {
                worthlessCrops.add(cropGroup);
            }
        }
        crops.removeAll(worthlessCrops);
    }

    public ArrayList<Farm> invest()
    {
        /**
         * There are no possible crops to plant for one of the following reasons:
         *      There are no crops that will grow (or regrow) before the end of the season
         *      You don't have enough money to buy the least expensive crop
         *      You have maxed out the number of crops you can water in a day without hitting 0 energy
         *
         * In any of these cases, return this farm progressed one day.
         */
        if (cropTypes.size() == 0 ||
            gold < cropTypes.get(cropTypes.size()-1).getBuyPrice() ||
            getNumCrops() >= Energy.maxWaterableTiles())
        {
            ArrayList<Farm> noPermutation = new ArrayList<>();
            event.setEndingGold(gold, goldCache);
            event.setSeedsPurchased(null);
            noPermutation.add(this);
            return noPermutation;
        }
        //calculate all permutations of farms
        else
        {
            FarmPermutation permutation = new FarmPermutation(this, cropTypes); //TODO no need to pass in cropTypes, and make static function
            return permutation.calculateFarmPermutations();
        }
    }

    //returns the number of crops currently on this farm
    public int getNumCrops()
    {
        int sum = 0;
        for (CropGroup cropGroup : crops)
        {
            sum += cropGroup.getNumber();
        }
        return sum;
    }

    public ArrayList<CropGroup> getCrops()
    {
        return this.crops;
    }

    public int getGold()
    {
        return this.gold;
    }

    public int getGoldCache()
    {
        return this.goldCache;
    }

    public ArrayList<FarmEvent> getEvents()
    {
        return this.events;
    }

    public void print()
    {
        for (FarmEvent event : events)
        {
            event.printStrategy();
        }
    }

    //sorts the farms by order of profit
    @Override
    public int compareTo(Farm other)
    {
        return other.gold - this.gold;
    }
}
