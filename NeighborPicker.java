import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

public class NeighborPicker {
    List<Neighbor> neighbors;

    NeighborPicker() {
        neighbors = new ArrayList<Neighbor>();
    }
    NeighborPicker(List<Neighbor> neighbors_) {
        neighbors = new ArrayList<Neighbor>();
        for (Neighbor n : neighbors_) if(n.interested) neighbors.add(n);
    }
    NeighborPicker(Set<Map.Entry<Integer, Neighbor>> neighbors_) {
        neighbors = new ArrayList<Neighbor>();
        for (Map.Entry<Integer, Neighbor> n : neighbors_) {
            if(n.getValue().interested) neighbors.add(n.getValue());
        }
    }

    public void add(Neighbor n) {
        neighbors.add(n);
    }

    public Neighbor getMax() { // If no valid neighbor, return null;
        float maxRate = -1;   //rate should never be negative

        ArrayList<Neighbor> equals = new ArrayList<Neighbor>();

        //loop through the neighbors
        for(Neighbor n : neighbors)
        {
            if(n.getRate() == maxRate)
            {
                equals.add(n);
            }
            else if(n.getRate() > maxRate)
            {
                //update values
                equals.clear();
                maxRate = n.getRate();

                //start a new duplicate chain
                equals.add(n);
            }
        }

        Neighbor maxNeighbor =  null;
        //choose a random index from the maximum rates
        if(equals.size() > 2)
        {
            int randomNum = ThreadLocalRandom.current().nextInt(0, equals.size());
            maxNeighbor = equals.get(randomNum);
            neighbors.remove(maxNeighbor);
        }
        if (equals.size() == 1) {
            maxNeighbor = equals.get(0);
            neighbors.remove(maxNeighbor);
        }
        return maxNeighbor;
    }

    public List<Neighbor> getMaxPreffered(int count) {
        List<Neighbor> maxs = new ArrayList<Neighbor>();
        while (maxs.size() < count) {
            Neighbor n = getMax();
            if (n==null) break;
            n.preferred = true;
            maxs.add(n);
        }
        return maxs;
    }

    public Neighbor getRandom() { // If no valid neighbor, return null
        if (neighbors.size()==0) return null;
        int randomNum = ThreadLocalRandom.current().nextInt(0, neighbors.size());
        Neighbor r = neighbors.get(randomNum);
        neighbors.remove(r);
        return r;
    }

    public List<Neighbor> getRandomPreffered(int count) {
        List<Neighbor> randos = new ArrayList<Neighbor>();
        while (randos.size() < count) {
            Neighbor n = getRandom();
            if (n==null) break;
            n.preferred = true;
            randos.add(n);
        }
        return randos;
    }
}
