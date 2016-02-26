package ucles.weblab.common.files.webapi;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import ucles.weblab.common.blob.api.Blob;
import ucles.weblab.common.blob.api.BlobId;
import ucles.weblab.common.blob.api.BlobNotFoundException;
import ucles.weblab.common.blob.api.BlobStoreException;
import ucles.weblab.common.blob.api.BlobStoreResult;
import ucles.weblab.common.blob.api.BlobStoreService;
import ucles.weblab.common.files.domain.SecureFile;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 *
 * @author Sukhraj
 */
public class FileDownloadCacheS3 implements FileDownloadCache<UUID, PendingDownload> {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    //private final ConcurrentHashMap<String, String> recentFileNamesToUrls;        
    private final BlobStoreService blobStoreService;
    private Clock clock = Clock.systemUTC();
    private Duration cacheExpiry;
    
    public FileDownloadCacheS3(BlobStoreService blobStoreService) {
        this.blobStoreService = blobStoreService;
        //this.recentFileNamesToUrls = new ConcurrentHashMap<>();

    }

    @Override
    public void clean() {
        //do this later....
    }

    @Override
    public Duration getExpiry() {
        return cacheExpiry;
    }

    /**
     * Check if the item is already in S3.
     * @param id
     * @param collectionName
     * @param fileName
     * @return 
     */
    @Override
    public Optional<PendingDownload> get(UUID id, String collectionName, String fileName ) {                
        
        String fileNameToGet = createCacheEntryKey(id, collectionName, fileName);
        
        try {
            Optional<Blob> blob = blobStoreService.getBlob(new BlobId(fileNameToGet));
            
            if (blob.isPresent()) {
                Blob b = blob.get();
                PendingDownload pd = new PendingDownload(MediaType.valueOf(b.getMimeType()), 
                                                         id.toString(), 
                                                         b.getData(),
                                                         b.getExpiryDate(),
                                                         b.getUrl());
                
                return Optional.of(pd);
            }
            
        } catch (BlobStoreException | BlobNotFoundException ex) {
            
            log.warn("Exception thrown when getting blob with id", ex);
            return Optional.empty();
        } 
        return Optional.empty();
    }

    @Override
    public Optional<BlobStoreResult> put(UUID id, String collectionName, PendingDownload pendingDownload) {
        String fileNameToStore = createCacheEntryKey(id, collectionName, pendingDownload.getFilename());
                
        try {
            Optional<BlobStoreResult> result = blobStoreService.putBlob(new BlobId(fileNameToStore), pendingDownload.getContentType().toString(), pendingDownload.getContent(), pendingDownload.getPurgeTime());
            
            return result;                                       
            
        } catch (BlobStoreException e) {
            log.warn("Exception thrown when putting into S3 blob with id", e);
        }    
        return Optional.empty();
    }
    
    @Override
    public boolean exists(UUID id, String collectionName, SecureFile secureFile) {
        
        String fileNameToStore = createCacheEntryKey(id, collectionName, secureFile.getFilename());
        return blobStoreService.exists(new BlobId(fileNameToStore));
        
    }
    
    @Override
    public Optional<String> getUrl(UUID id, String collectionName, String fileName) {
        String s3fileName = createCacheEntryKey(id, collectionName, fileName);
        try {
            Optional<URI> uri = blobStoreService.getUrl(new BlobId(s3fileName));
            
            if (uri.isPresent()) {
                //recentFileNamesToUrls.put(collectionName + "_" + pendingDownload.getFilename(), uri.get().toString());
                return Optional.of(uri.get().toString());
            }
        } catch (BlobStoreException | BlobNotFoundException e) {
            log.warn("Exception thrown when getting into S3 blob with id", e);
        } 
        return Optional.empty();
    }
    
    /*@Override
    public Optional<String> getRecentUrl(String collectionName, String fileName) {
        String res = recentFileNamesToUrls.get(collectionName + "_" + fileName);
        if (res != null) {
            return Optional.of(res);
        } else {
            return Optional.ofNullable(res);
        }
    }*/
    
    @Autowired(required = false) // will fall back to default system UTC clock
    public void configureClock(Clock clock) {
        log.warn("Clock overridden with " + clock);
        this.clock = clock;
    }

    @Autowired
    void configureCacheExpiry(@Value("${files.download.cache.expirySeconds:30}") int cacheExpirySeconds) {
        log.info("Cache expiry set to " + cacheExpirySeconds + "s");
        this.cacheExpiry = Duration.ofSeconds(cacheExpirySeconds);
    }

    @Override
    public URI getRedirectUrl(UUID id, String collectionName, String fileName) {
        return linkTo(methodOn(DownloadController.class).redirectToExternalUrl(collectionName, fileName, id)).toUri();
    }
    
}