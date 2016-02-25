package ucles.weblab.common.files.webapi;

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Ignore;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import ucles.weblab.common.files.domain.EncryptionService;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.jpa.SecureFileCollectionEntityJpa;
import ucles.weblab.common.files.domain.jpa.SecureFileEntityJpa;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ucles.weblab.common.test.webapi.WebTestSupport.setUpRequestContext;

/**
 * @since 28/03/15
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloadControllerTest {
    
    private DownloadController downloadController;
    
    @Before
    public void setUp() {
        setUpRequestContext();
        
        FileDownloadCacheInMemory inMemoryCache = new FileDownloadCacheInMemory();
        inMemoryCache.configureCacheExpiry(3600);
        
        downloadController = new DownloadController(inMemoryCache);
        
    }

    @Test
    @Ignore
    public void testDownloadCacheWorks() {
        byte[] content = new byte[] { 1, 5, 3, 8, 1, 7, 3, 9, 4, 6, 7 };
        MediaType contentType = MediaType.IMAGE_GIF;
        String filename = "nonsense picture.gif";
        
        SecureFileEntity secureFileEntity = mockSecureFile(filename);
        
                        
        final URI uri = downloadController.generateDownload(filename, secureFileEntity);
        //assertTrue("The URI should be set", uri.toString().startsWith("http://localhost/downloads/"));
        final UUID uuid = UUID.fromString(uri.toString().substring(11));
        final ResponseEntity<byte[]> response = downloadController.fetchPreviouslyGeneratedDownload(uuid.toString());
        assertEquals("Should return 200 OK", HttpStatus.OK, response.getStatusCode());
        //assertArrayEquals("Expect content returned", content, response.getBody());
        assertFalse("Must not have no-cache", response.getHeaders().getCacheControl().contains("no-cache"));
        assertFalse("Must not have no-store", response.getHeaders().getCacheControl().contains("no-store"));
        assertEquals("Expect content type returned", contentType, response.getHeaders().getContentType());
        assertEquals("Expect content length returned", (long) content.length, response.getHeaders().getContentLength());
        assertThat("Expect filename return", response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION), contains(containsString(filename)));
        assertThat("Expect filename to be quoted", response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION), contains(containsString('"' + filename + '"')));
    }
    
    /**
     * Creates a SecureFile entity with the details of the mock multipart file provided.
     *
     * @param file the mock multipart file
     * @return entity with filename, content type and length populated.
     */
    public static SecureFile mockSecureFile(MockMultipartFile file) {
        final SecureFile savedFile = mock(SecureFile.class);
        when(savedFile.getFilename()).thenReturn(file.getOriginalFilename());
        when(savedFile.getContentType()).thenReturn(file.getContentType());
        when(savedFile.getLength()).thenReturn(file.getSize());
        return savedFile;
    }

    /**
     * Creates a SecureFile entity with the details provided.
     *
     * @param filename the filename to return
     * @return entity with details populated
     */
    public static SecureFileEntity mockSecureFile(final String filename) {
        final SecureFileEntity file = mock(SecureFileEntity.class);
        when(file.getFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn(MediaType.IMAGE_GIF.toString());
        when(file.getPlainData()).thenReturn(new byte[] { 1, 5, 3, 8, 1, 7, 3, 9, 4, 6, 7 });
        return file;
    }

    /**
     * Creates a SecureFile entity with the details provided.
     *
     * @param filename the filename to return
     * @param collection the collection the file is part of
     * @return entity with details populated
     */
    public static SecureFileEntity mockSecureFile(final String filename, SecureFileCollectionEntity collection) {
        SecureFileEntity file = mockSecureFile(filename);
        when(file.getCollection()).thenReturn(collection);
        return file;
    }

    public static SecureFileCollectionEntity mockSecureFileCollection(final String displayName) {
        SecureFileCollectionEntity collection = mock(SecureFileCollectionEntity.class);
        when(collection.getDisplayName()).thenReturn(displayName);
        when(collection.getBucket()).thenReturn("fs." + displayName.toLowerCase().replaceAll("\\W", ""));
        when(collection.getPurgeInstant()).thenReturn(Optional.empty());
        return collection;
    }
    
    class SecureFileCollectionTestVO implements SecureFileCollectionEntity {

        @Override
        public String getDisplayName() {
            return "some-display-name";
        }

        @Override
        public Optional<Instant> getPurgeInstant() {
            return Optional.of(Instant.MAX);
        }

        @Override
        public String getId() {
            return "id";
        }

        @Override
        public String getBucket() {
            return deriveBucket(getDisplayName());
        }
        
    }
    
    class SecureFileTestVO implements SecureFileEntity {

        private String contentType;
        private String fileName;
        private String notes;   
        private int length;
        private byte[] data;

        public SecureFileTestVO(String contentType, String fileName, String notes, int length, byte[] data) {
            this.contentType = contentType;
            this.fileName = fileName;
            this.notes = notes;
            this.length = length;
            this.data = data;
        }
        
        @Override
        public SecureFileCollectionEntity getCollection() {
            return new SecureFileCollectionTestVO();
        }

        @Override
        public boolean isNew() {
            return true;
        }

        @Override
        public void setFilename(String filename) {
            this.fileName = filename;
        }

        @Override
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public void setNotes(String notes) {
            this.notes = notes;
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public String getNotes() {
            return notes;
        }

        @Override
        public byte[] getPlainData() {
            return data;
        }
        
    }
}
