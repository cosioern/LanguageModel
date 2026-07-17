package com.cosio.lm;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, UUID> {
    List<Guest> findByLastUpdatedAtBefore(Instant cutoff);
}
