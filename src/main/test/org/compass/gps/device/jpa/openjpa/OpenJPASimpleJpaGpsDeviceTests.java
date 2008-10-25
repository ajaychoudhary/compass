package org.compass.gps.device.jpa.openjpa;

import java.util.HashMap;
import javax.persistence.EntityManagerFactory;

import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.compass.core.util.JdkVersion;
import org.compass.gps.device.jpa.AbstractSimpleJpaGpsDeviceTests;
import org.compass.gps.device.jpa.JpaGpsDevice;

/**
 * Performs JPA tests using OpenJPA specific support.
 *
 * @author kimchy
 */
public class OpenJPASimpleJpaGpsDeviceTests extends AbstractSimpleJpaGpsDeviceTests {

    @Override
    protected void addDeviceSettings(JpaGpsDevice device) {
        device.setInjectEntityLifecycleListener(true);
    }

    protected EntityManagerFactory doSetUpEntityManagerFactory() {
        return new PersistenceProviderImpl().createEntityManagerFactory("openjpa", new HashMap());
    }

    public void testMirror() throws Exception {
        // TODO OpenJPA fails on this with JDK 6, strange!
        if (JdkVersion.isAtLeastJava16()) {
            return;
        }
        super.testMirror();
    }
}
