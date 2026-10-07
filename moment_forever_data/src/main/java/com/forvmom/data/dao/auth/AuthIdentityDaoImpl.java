package com.forvmom.data.dao.auth;

import com.forvmom.common.enums.AuthIdentityProvider;
import com.forvmom.data.dao.GenericDaoImpl;
import com.forvmom.data.entities.auth.AuthIdentity;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class AuthIdentityDaoImpl extends GenericDaoImpl<AuthIdentity, Long> implements AuthIdentityDao {

    public AuthIdentityDaoImpl() {
        super(AuthIdentity.class);
    }

    @Override
    public Optional<AuthIdentity> findByProviderAndProviderSubject(AuthIdentityProvider provider, String providerSubject) {
        try {
            AuthIdentity identity = em.createQuery(
                    "SELECT ai FROM AuthIdentity ai WHERE ai.provider = :provider AND ai.providerSubject = :providerSubject",
                    AuthIdentity.class)
                    .setParameter("provider", provider)
                    .setParameter("providerSubject", providerSubject)
                    .getSingleResult();
            return Optional.of(identity);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthIdentity> findByProviderAndProviderSubjectIncludingDeleted(AuthIdentityProvider provider,
            String providerSubject) {
        try {
            AuthIdentity identity = (AuthIdentity) em.createNativeQuery(
                            "SELECT * FROM auth_identities WHERE provider = :provider AND provider_subject = :providerSubject LIMIT 1",
                            AuthIdentity.class)
                    .setParameter("provider", provider.name())
                    .setParameter("providerSubject", providerSubject)
                    .getSingleResult();
            return Optional.of(identity);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthIdentity> findByAuthUserIdAndProvider(Long authUserId, AuthIdentityProvider provider) {
        try {
            AuthIdentity identity = em.createQuery(
                    "SELECT ai FROM AuthIdentity ai WHERE ai.authUser.id = :authUserId AND ai.provider = :provider",
                    AuthIdentity.class)
                    .setParameter("authUserId", authUserId)
                    .setParameter("provider", provider)
                    .getSingleResult();
            return Optional.of(identity);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
