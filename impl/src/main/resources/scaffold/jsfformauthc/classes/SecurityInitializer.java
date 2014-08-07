/*
 ** JBoss, Home of Professional Open Source
 ** Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 ** contributors by the @authors tag. See the copyright.txt in the
 ** distribution for a full listing of individual contributors.
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** http://www.apache.org/licenses/LICENSE-2.0
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 **/
package __package_name;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.User;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 ** This startup bean creates a default user account when the application is started. Since we are not
 ** providing an IDM configuration in this example, PicketLink will default to using a jpa-based identity
 ** store to persist user and other identity state.
 **/
@Singleton
@Startup
public class SecurityInitializer {

    @Inject
    private PartitionManager partitionManager;

    @PostConstruct
    public void create() {
        User user = new User("picketlink");

        user.setEmail("picketlink@jboss.org");
        user.setFirstName("PicketLink");
        user.setLastName("User");

        IdentityManager identityManager = this.partitionManager.createIdentityManager();

        identityManager.add(user);
        identityManager.updateCredential(user, new Password("picketlink"));
    }
}
