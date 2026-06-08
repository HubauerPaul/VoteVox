package at.htlwels.votevox.schoolclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ElectionClassRepository extends JpaRepository<ElectionClass, UUID> {

    List<ElectionClass> findAllByElectionId(UUID electionId);

    boolean existsByElectionIdAndSchoolClassId(UUID electionId, UUID classId);

    long countByElectionId(UUID electionId);

    void deleteByElectionId(UUID electionId);

    /** Sum of the student counts of all classes selected for the election. */
    @Query("SELECT COALESCE(SUM(ec.schoolClass.studentCount), 0) "
         + "FROM ElectionClass ec WHERE ec.election.id = :electionId")
    long sumStudentCountByElectionId(UUID electionId);
}
