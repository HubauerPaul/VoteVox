package at.htlwels.votevox.election;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ElectionRepository extends JpaRepository<Election, UUID> {

    List<Election> findAllByOrderByStartTimeDesc();

    List<Election> findAllByStatus(ElectionStatus status);
}
