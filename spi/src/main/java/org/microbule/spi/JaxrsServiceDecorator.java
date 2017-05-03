package org.microbule.spi;

import org.microbule.config.api.Config;

public interface JaxrsServiceDecorator {
//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    String name();

    void decorate(JaxrsServiceDescriptor descriptor, Config config);
}
