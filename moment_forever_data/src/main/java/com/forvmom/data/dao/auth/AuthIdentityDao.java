package com.forvmom.data.dao.auth;

import com.forvmom.common.enums.AuthIdentityProvider;
import com.forvmom.data.dao.GenericDao;
import com.forvmom.data.entities.auth.AuthIdentity;

import java.util.Optional;

public interface AuthIdentityDao extends GenericDao<AuthIdentity, Long> {

    Optional<AuthIdentity> findByProviderAndProviderSubject(AuthIdentityProvider provider, String providerSubject);

    Optional<AuthIdentity> findByProviderAndProviderSubjectIncludingDeleted(AuthIdentityProvider provider,
            String providerSubject);

    Optional<AuthIdentity> findByAuthUserIdAndProvider(Long authUserId, AuthIdentityProvider provider);
}
