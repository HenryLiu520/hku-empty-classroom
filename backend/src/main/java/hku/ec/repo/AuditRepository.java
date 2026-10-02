package hku.ec.repo;

import hku.ec.domain.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {
    List<AuditEntry> findTop100ByOrderByCreatedAtDesc();
}
