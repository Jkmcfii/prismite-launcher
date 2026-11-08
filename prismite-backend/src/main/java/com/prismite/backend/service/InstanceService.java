package com.prismite.backend.service;

import com.prismite.backend.model.Instance;
import com.prismite.backend.model.User;
import com.prismite.backend.repository.InstanceRepository;
import com.prismite.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstanceService {
    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;

    public InstanceService(InstanceRepository instanceRepository, UserRepository userRepository) {
        this.instanceRepository = instanceRepository;
        this.userRepository = userRepository;
    }

    public List<Instance> getInstancesByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return instanceRepository.findByUser(user);
    }

    public Instance createInstance(String email, String name, String version, String loader, String gameDirectory, String javaPath, String memory) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Instance instance = new Instance(user, name, version, loader);
        instance.setGameDirectory(gameDirectory);
        instance.setJavaPath(javaPath);
        instance.setMemory(memory);

        return instanceRepository.save(instance);
    }
}
