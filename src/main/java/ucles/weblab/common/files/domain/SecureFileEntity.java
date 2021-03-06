package ucles.weblab.common.files.domain;

import java.time.Instant;

/**
 * Persistence-technology-neutral interface representing a persistable secure file in a collection.
 *
 * @since 05/06/15
 */
public interface SecureFileEntity extends MutableSecureFile, SecureFileMetadataEntity {
    SecureFileCollectionEntity getCollection();

    boolean isNew();
    
    Instant getCreatedDate();
}
