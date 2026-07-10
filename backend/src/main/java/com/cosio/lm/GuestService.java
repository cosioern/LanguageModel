package com.cosio.lm;

import java.util.UUID;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GuestService {
    
    private final GuestRepository repo;
    public GuestService(GuestRepository repo) {
        this.repo = repo;
    }

    public UUID createGuest() {
        Guest g = new Guest();
        repo.save(g);
        return g.getGuestID();
    }

    public Guest findGuest(UUID guestID) {
        Optional<Guest> g = repo.findById(guestID);
        if (g.isPresent()) {
            return g.get();
        }
        return null;
    }

    public void updateLastSeen(Guest g) {
        g.updateLastSeen();
        repo.save(g);
        return;
    }

}
