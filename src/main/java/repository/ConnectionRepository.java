package com.divya.linkedinclone.repository;

import com.divya.linkedinclone.entity.Connection;
import com.divya.linkedinclone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findBySenderAndReceiver(User sender, User receiver);
    List<Connection> findByReceiverAndStatus(User receiver, Connection.ConnectionStatus status);
    List<Connection> findBySenderOrReceiver(User sender, User receiver);
    List<Connection> findBySender(User sender);
    List<Connection> findByReceiver(User receiver);
}