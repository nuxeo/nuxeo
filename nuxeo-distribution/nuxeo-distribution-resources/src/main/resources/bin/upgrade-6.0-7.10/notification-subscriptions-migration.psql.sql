CREATE extension "uuid-ossp";
CREATE OR REPLACE FUNCTION nx_migrate_notif()
RETURNS integer
AS $$
DECLARE
    subscription RECORD;
    notification RECORD;
    username RECORD;
    notif_uuid VARCHAR;
    POS1 INTEGER;
    POS2 INTEGER;
    CNT INTEGER;
    TOTALCNT INTEGER;
BEGIN

    TOTALCNT:=0;
    RAISE INFO 'Migrating usersubscription table to document facet';

    RAISE INFO 'Removing orphan notifications';
    DELETE FROM usersubscription WHERE docid IN (
        SELECT distinct us.docid
          FROM usersubscription AS us LEFT OUTER JOIN hierarchy AS h ON h.id = us.docid
         WHERE h.id IS NULL
    );

    -- Iterate over all docId that are part of subscriptions
    FOR subscription  IN SELECT DISTINCT docid, mixintypes
                           FROM usersubscription AS us,
                                hierarchy AS h
                          WHERE us.docid = h.id LOOP

      -- Add the 'Notifiable' facet if needed
      IF NOT 'Notifiable' = ANY (subscription.mixintypes::varchar[]) THEN
        UPDATE hierarchy SET mixintypes = array_append(mixintypes,'Notifiable') WHERE id = subscription.docid;
      END IF;

      SELECT MAX(POS) INTO POS1 FROM hierarchy
       WHERE hierarchy.id = subscription.docid
         AND hierarchy.name = 'notif:notifications'
         AND primaryType = 'notificationEntry';

      RAISE INFO 'Migrating subscription on doc : % ', subscription.docid;
      FOR notification IN SELECT DISTINCT us.notification FROM usersubscription AS us WHERE docid = subscription.docid LOOP

        -- For each notification on the doc we try to fetch if there are existing notificationEntry
        -- for this notification
        SELECT DISTINCT notificationEntry.id INTO notif_uuid
          FROM hierarchy,notificationEntry
          WHERE notificationEntry.id = hierarchy.id
            AND hierarchy.parentId = subscription.docID
            AND notificationEntry.name =  notification.notification;

        IF notif_uuid IS NULL THEN
          -- If not, we create the hierarchy node and its notificationEntry
          SELECT uuid_generate_v4() INTO notif_uuid;
          INSERT INTO hierarchy (id, parentid, pos, name, isproperty, primarytype) VALUES
          (notif_uuid, subscription.docid, POS1, 'notif:notifications', true, 'notificationEntry');
          INSERT INTO notificationEntry (id, name) VALUES (notif_uuid, notification.notification);
        ELSE
          RAISE WARNING 'Notification % already in hierachy', notification.notification;
        END IF;

        -- Then we insert the usernames that are subscribed to this notification it they're not already subscribed
        SELECT MAX(POS) INTO POS2 FROM subscribers WHERE id = notif_uuid;
        FOR username IN  SELECT DISTINCT us.userid
                           FROM usersubscription AS us
                          WHERE us.docid = subscription.docid
                            AND us.notification = notification.notification LOOP

          SELECT COUNT(*) INTO CNT
            FROM subscribers
           WHERE id = notif_uuid
             AND item=username.userid;

          IF CNT = 0 THEN
            INSERT INTO subscribers (id, pos, item) VALUES (notif_uuid, POS2  , username.userid);
            TOTALCNT := TOTALCNT + 1;
            SELECT POS2+1 INTO POS2;
          ELSE
            RAISE WARNING 'Username % already subscribed', username.userid;
          END IF;
        END LOOP;

        SELECT POS1+1 INTO POS1;
      END LOOP;
      DELETE FROM usersubscription WHERE docid = subscription.docid;

    END LOOP;

    RAISE INFO 'Migrated % subscriptions', TOTALCNT;
    RAISE INFO 'Done migrating usersubscription table';
    RETURN 1;
END $$
LANGUAGE plpgsql;

select nx_migrate_notif();

-- CLEANING
DROP FUNCTION nx_migrate_notif();
DROP extension "uuid-ossp";
