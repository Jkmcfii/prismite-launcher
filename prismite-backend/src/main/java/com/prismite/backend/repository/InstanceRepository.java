package com.prismite.backend.repository;

import com.prismite.backend.model.Instance;
import com.prismite.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstanceRepository extends JpaRepository<Instance, Long> {
    List<Instance> findByUser(User user);
}
