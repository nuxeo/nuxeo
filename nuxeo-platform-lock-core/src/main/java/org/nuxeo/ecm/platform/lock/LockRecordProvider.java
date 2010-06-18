package org.nuxeo.ecm.platform.lock;

import java.net.URI;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class LockRecordProvider {

    protected EntityManager em;

    public LockRecordProvider(EntityManager em) {
        this.em = em;
    }

    public void delete(URI resource) {
        Query query = em.createNamedQuery("Lock.deleteByResource");
        query.setParameter("resource", resource);
        query.executeUpdate();
    }

    public void delete(LockRecord record) {
        em.remove(record);
    }

    public LockRecord getRecord(URI resourceUri) {
        Query query = em.createNamedQuery("Lock.findByResource");
        query.setParameter("resource", resourceUri);
        return (LockRecord) query.getSingleResult();
    }

    public LockRecord refreshRecord(LockRecord record) {
        em.refresh(record);
        return record;
    }

    public LockRecord updateRecord(URI self, URI resource, String comments,
            long timeout) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + timeout);
        Query query = em.createNamedQuery("Lock.findByResource");
        query.setParameter("resource", resource);
        LockRecord record = (LockRecord) query.getSingleResult();
        record.owner = self;
        record.comments = comments;
        record.lockTime = now;
        record.expireTime = expire;
        return record;
    }

    public LockRecord createRecord(URI self, URI resource, String comment,
            long timeout) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + timeout);
        LockRecord record = new LockRecord(self, resource, comment, now, expire);
        em.persist(record);
        return record;
    }

}
