package com.cursokotlin.routemapexample;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class TSP {

    private int numLocations;
    private List<LatLng> locations = new ArrayList<>();
    private int routes[][];
    private float distances[];

    private static final float pC = 0.80f;
    private static final float pM = 0.01f;

    public List<LatLng> getTSP(@NonNull List<LatLng> _locations){
        locations.clear();
        locations = _locations;

        numLocations = locations.size();
        routes = new int[10][numLocations];
        distances = new float[10];

        initializePopulation();
        int bestRoute[] = geneticAlgorithm();
        List<LatLng> result = new ArrayList<>();

        for (int i = 0; i < numLocations; i++) {
            result.add(locations.get(bestRoute[i]));
        }

        return result;
    }

    private void initializePopulation() {
        for (int i = 0; i < 10; i++) {
            routes[i][0] = 0;
            for (int j = 1; j < numLocations; j++) {
                boolean exists;
                int location = 0;
                do {
                    exists = false;
                    location = (int)(Math.random() * numLocations);
                    for (int k = 0; k < j; k++) {
                        if (location == routes[i][k]) {
                            exists = true;
                            break;
                        }
                    }
                } while (exists);
                routes[i][j] = location;
            }
            distances[i] = calculateDistance(routes[i]);
        }
    }

    private float calculateDistance(int[] route) {
        float distance = 0f;
        for (int i = 0; i < numLocations - 1; i++) {
            LatLng start = locations.get(route[i]);
            LatLng end = locations.get(route[i + 1]);
            distance += distanceBetweenPoints(start, end);
        }
        distance += distanceBetweenPoints(locations.get(route[numLocations - 1]), locations.get(0));
        return distance;
    }

    private float distanceBetweenPoints(LatLng point1, LatLng point2) {
        double lat1 = point1.latitude;
        double lon1 = point1.longitude;
        double lat2 = point2.latitude;
        double lon2 = point2.longitude;
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (earthRadius * c);
    }

    private int selectTournament() {
        int participant1, participant2;

        participant1 = (int)(Math.random() * 10);
        do {
            participant2 = (int)(Math.random() * 10);
        } while (participant1 == participant2);

        if (distances[participant1] < distances[participant2]) {
            return participant1;
        } else {
            return participant2;
        }
    }

    private int[] geneticAlgorithm() {
        int[] bestRoute = new int[numLocations];

        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 1000; z++) {
                int parent1Pos, parent2Pos;

                parent1Pos = selectTournament();
                do {
                    parent2Pos = selectTournament();
                } while (parent1Pos == parent2Pos);

                int parent1[] = routes[parent1Pos];
                int parent2[] = routes[parent2Pos];

                int child1[] = new int[numLocations];
                int child2[] = new int[numLocations];

                float currentPC = (float) Math.random();

                if (currentPC < pC) {
                    int limit = (int) (Math.random() * (numLocations / 2)) + 2;
                    for (int i = 0; i <= limit; i++) {
                        child1[i] = parent1[i];
                        child2[i] = parent2[i];
                    }

                    boolean contains;
                    int current = limit + 1, current2 = limit + 1;
                    for (int i = 0; i < numLocations; i++) {
                        contains = false;
                        for (int j = 0; j < limit + 1; j++) {
                            if (parent2[i] == child1[j]) {
                                contains = true;
                                break;
                            }
                        }
                        if (!contains) {
                            child1[current] = parent2[i];
                            current++;
                        }

                        contains = false;
                        for (int j = 0; j < limit + 1; j++) {
                            if (parent1[i] == child2[j]) {
                                contains = true;
                                break;
                            }
                        }
                        if (!contains) {
                            child2[current2] = parent1[i];
                            current2++;
                        }
                    }
                } else {
                    child1 = parent1;
                    child2 = parent2;
                }

                float currentPM = (float) Math.random();

                if (currentPM < pM) {
                    int temp, pos1, pos2;

                    pos1 = ((int) (Math.random() * numLocations) % (numLocations - 1)) + 1;
                    do {
                        pos2 = ((int) (Math.random() * numLocations) % (numLocations - 1)) + 1;
                    } while (pos1 == pos2);

                    temp = child1[pos1];
                    child1[pos1] = child1[pos2];
                    child1[pos2] = temp;

                    pos1 = ((int) (Math.random() * numLocations) % (numLocations - 1)) + 1;
                    do {
                        pos2 = ((int) (Math.random() * numLocations) % (numLocations - 1)) + 1;
                    } while (pos1 == pos2);

                    temp = child2[pos1];
                    child2[pos1] = child2[pos2];
                    child2[pos2] = temp;
                }

                float[] newDistances = new float[4];
                newDistances[0] = calculateDistance(parent1);
                newDistances[1] = calculateDistance(parent2);
                newDistances[2] = calculateDistance(child1);
                newDistances[3] = calculateDistance(child2);

                int[] pos = {0, 1, 2, 3};

                float aux1;
                int aux2;
                for (int i = 0; i < newDistances.length - 1; i++) {
                    for (int j = 0; j < newDistances.length - i - 1; j++) {
                        if (newDistances[j + 1] < newDistances[j]) {
                            aux1 = newDistances[j + 1];
                            newDistances[j + 1] = newDistances[j];
                            newDistances[j] = aux1;

                            aux2 = pos[j + 1];
                            pos[j + 1] = pos[j];
                            pos[j] = aux2;
                        }
                    }
                }

                switch (pos[0]) {
                    case 0:
                        routes[parent1Pos] = parent1;
                        break;
                    case 1:
                        routes[parent1Pos] = parent2;
                        break;
                    case 2:
                        routes[parent1Pos] = child1;
                        break;
                    case 3:
                        routes[parent1Pos] = child2;
                        break;
                }

                switch (pos[1]) {
                    case 0:
                        routes[parent2Pos] = parent1;
                        break;
                    case 1:
                        routes[parent2Pos] = parent2;
                        break;
                    case 2:
                        routes[parent2Pos] = child1;
                        break;
                    case 3:
                        routes[parent2Pos] = child2;
                        break;
                }

                for (int i = 0; i < 10; i++) {
                    distances[i] = calculateDistance(routes[i]);
                }
            }
        }

        int best = 0;
        for (int i = 0; i < 10; i++) {
            if (distances[best] > distances[i]) {
                best = i;
            }
        }

        return routes[best];
    }
}
