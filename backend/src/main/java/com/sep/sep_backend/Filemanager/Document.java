package com.sep.sep_backend.Filemanager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.sep_backend.travelentry.TravelEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A file attached to a travel entry, stored in Cloudflare R2.
 */
@Getter @Setter
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // Holds the R2 object key (userId/uuid_filename), not a URL.
    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_entry_id", nullable = false)
    @JsonIgnore // prevents infinite recursion during JSON serialization
    private TravelEntry travelEntry;

}
