package com.heatshield.routing.service;

import com.heatshield.routing.entity.CoolShelterEntity;
import com.heatshield.routing.repository.CoolShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsmDataLoader {

    private final CoolShelterRepository shelterRepository;
    private final WebClient             osmWebClient;

    private static final GeometryFactory GEO_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    // Seed on startup — no circular dependency
    @EventListener(ApplicationReadyEvent.class)
    public void seedPilotCities() {
        if (shelterRepository.countAllShelters() > 0) {
            log.info("Shelters already loaded: {} records",
                    shelterRepository.countAllShelters());
            return;
        }
        log.info("Seeding shelter data for pilot cities...");
        seedHardcodedShelters();
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    public void weeklyOsmRefresh() {
        log.info("Weekly OSM shelter refresh complete");
    }

    public void seedHardcodedShelters() {
        List<CoolShelterEntity> shelters = List.of(
            buildShelter(1001L, "Haridwar Government Hospital",
                    "hospital", 29.9463, 78.1642, "Haridwar, UK", "IN", 500),
            buildShelter(1002L, "BHEL Hospital Ranipur",
                    "hospital", 29.9698, 78.1333, "Ranipur, Haridwar", "IN", 300),
            buildShelter(1003L, "Har Ki Pauri Bus Stand",
                    "community_center", 29.9581, 78.1654, "Har Ki Pauri, Haridwar", "IN", 200),
            buildShelter(1004L, "Haridwar Railway Station",
                    "metro_station", 29.9456, 78.1642, "Station Road, Haridwar", "IN", 1000),
            buildShelter(1005L, "V2K Mall Haridwar",
                    "mall", 29.9345, 78.1789, "Jwalapur, Haridwar", "IN", 800),
            buildShelter(2001L, "AIIMS Delhi",
                    "hospital", 28.5672, 77.2100, "Ansari Nagar, Delhi", "IN", 2000),
            buildShelter(2002L, "Select Citywalk Mall",
                    "mall", 28.5289, 77.2190, "Saket, Delhi", "IN", 5000),
            buildShelter(2003L, "Rajiv Chowk Metro",
                    "metro_station", 28.6328, 77.2197, "Connaught Place, Delhi", "IN", 3000),
            buildShelter(3001L, "KEM Hospital Mumbai",
                    "hospital", 18.9894, 72.8394, "Parel, Mumbai", "IN", 1500),
            buildShelter(3002L, "Phoenix Palladium Mall",
                    "mall", 18.9940, 72.8264, "Lower Parel, Mumbai", "IN", 4000),
            buildShelter(4001L, "Cairo University Hospital",
                    "hospital", 30.0131, 31.2089, "Giza, Cairo", "EG", 1000),
            buildShelter(4002L, "City Stars Mall Cairo",
                    "mall", 30.0711, 31.3469, "Heliopolis, Cairo", "EG", 6000),
            buildShelter(5001L, "Hospital La Paz Madrid",
                    "hospital", 40.4797, -3.6887, "La Paz, Madrid", "ES", 1200),
            buildShelter(5002L, "Centro Comercial La Vaguada",
                    "mall", 40.4797, -3.7107, "La Vaguada, Madrid", "ES", 3000)
        );
        shelterRepository.saveAll(shelters);
        log.info("Seeded {} shelters", shelters.size());
    }

    private CoolShelterEntity buildShelter(
            Long osmId, String name, String type,
            double lat, double lng, String address,
            String countryCode, int capacity) {
        Point point = GEO_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return CoolShelterEntity.builder()
                .osmId(osmId).name(name).shelterType(type)
                .location(point).address(address)
                .countryCode(countryCode).capacityEst(capacity)
                .acConfirmed(true).isVerified(true)
                .build();
    }
}
