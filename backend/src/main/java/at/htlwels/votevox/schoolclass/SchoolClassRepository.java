package at.htlwels.votevox.schoolclass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    Optional<SchoolClass> findByName(String name);

    boolean existsByName(String name);

    List<SchoolClass> findAllByOrderByNameAsc();
}
