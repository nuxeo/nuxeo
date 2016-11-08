-- --------------------------------------------------------------------------
-- Name         : notification-subscriptions-migration.oracle.sql
-- Author       : Damien Metzler
-- Description  : Migrate the notifications from 6.0 to 7.10 scheme
-- Ammedments   :
--   When         Who              What
--   ===========  ===============  ==========================================
--   08-MAR-2016  Damien METZLER   Initial Creation
-- --------------------------------------------------------------------------

-- This methods need the conversion_api that can be found here :
-- https://oracle-base.com/dba/miscellaneous/conversion_api.sql


CREATE OR REPLACE FUNCTION RANDOM_STRING(l_size NUMBER)
RETURN VARCHAR2
AS 
  l_random       VARCHAR2 (12);
BEGIN
  l_random := '';
   FOR i IN 1..l_size LOOP
      l_random := l_random  || RPAD (conversion_api.to_hex (TRUNC (DBMS_RANDOM.VALUE (low => 1, high => 65535))),4,'0');
  END LOOP;
  RETURN l_random;
END;
/


CREATE OR REPLACE FUNCTION new_uuid
   RETURN VARCHAR2
AS
BEGIN




   RETURN    LOWER(RANDOM_STRING(2)
          || '-'
          || RANDOM_STRING(1)
          || '-'
          || RANDOM_STRING(1)
          || '-'
          || RANDOM_STRING(1)
          || '-'
          || RANDOM_STRING(3));
END;
/


CREATE OR REPLACE PROCEDURE nx_migrate_notif
IS
   TOTALCNT     PLS_INTEGER;
   CNT          PLS_INTEGER;
   POS1         PLS_INTEGER;
   POS2         PLS_INTEGER;
   notif_uuid   NOTIFICATIONENTRY.id%TYPE;
   l_seed       BINARY_INTEGER;

   CURSOR NOTIFIEDDOC
   IS
      SELECT DISTINCT docid, mixintypes
        FROM usersubscription, hierarchy
       WHERE usersubscription.docid = hierarchy.id;
BEGIN
   TOTALCNT := 0;
   DBMS_OUTPUT.put_line (
      'Migrating usersubscription table to document facet');

   DBMS_OUTPUT.put_line ('Removing orphan notifications');

   DELETE FROM usersubscription
         WHERE docid IN
                  (SELECT DISTINCT usersubscription.docid
                     FROM usersubscription
                          LEFT OUTER JOIN hierarchy
                             ON hierarchy.id = usersubscription.docid
                    WHERE hierarchy.id IS NULL);

   FOR subscription IN NOTIFIEDDOC
   LOOP
      -- Add the 'Notifiable' facet if needed
      SELECT COUNT (*)
        INTO CNT
        FROM hierarchy
       WHERE     hierarchy.ID = subscription.docid
             AND hierarchy.MIXINTYPES LIKE '%|Notifiable|%';

      IF CNT = 0
      THEN
         DBMS_OUTPUT.put_line (
            'Adding Notifiable facet to ' || subscription.docid);

         IF subscription.mixintypes IS NULL OR LENGTH (subscription.mixintypes) = 0
         THEN
            UPDATE hierarchy
               SET mixintypes = '|Notifiable|'
             WHERE id = subscription.docid;
         ELSE
            UPDATE hierarchy
               SET mixintypes = mixintypes || ('Notifiable|')
             WHERE id = subscription.docid;
         END IF;
      END IF;

      SELECT MAX (POS)
        INTO POS1
        FROM hierarchy
       WHERE     hierarchy.id = subscription.docid
             AND hierarchy.name = 'notif:notifications'
             AND primaryType = 'notificationEntry';
       
    IF POS1 IS NULL THEN
      POS1 := 0;
    END IF;

      DBMS_OUTPUT.put_line (
         'Migrating subscription on doc : ' || subscription.docid);

      FOR notification IN (SELECT DISTINCT usersubscription.notification
                             FROM usersubscription
                            WHERE docid = subscription.docid)
      LOOP
         -- For each notification on the doc we try to fetch if there are existing notificationEntry
         -- for this notification
         BEGIN
            SELECT DISTINCT notificationEntry.id
              INTO notif_uuid
              FROM hierarchy, notificationEntry
             WHERE     notificationEntry.id = hierarchy.id
                   AND hierarchy.parentId = subscription.docID
                   AND notificationEntry.name = notification.notification;
         EXCEPTION
            WHEN NO_DATA_FOUND
            THEN
               notif_uuid := NULL;
         END;

         IF notif_uuid IS NULL
         THEN
            -- If not, we create the hierarchy node and its notificationEntry
            notif_uuid := new_uuid ();

            INSERT INTO hierarchy (id,
                                   parentid,
                                   pos,
                                   name,
                                   isproperty,
                                   primarytype)
                 VALUES (notif_uuid,
                         subscription.docid,
                         POS1,
                         'notif:notifications',
                         1,
                         'notificationEntry');

            INSERT INTO notificationEntry (id, name)
                 VALUES (notif_uuid, notification.notification);
         ELSE
            DBMS_OUTPUT.put_line (
                  'WARNING : Notification % already in hierachy'
               || notification.notification);
         END IF;

         -- Then we insert the usernames that are subscribed to this notification it they're not already subscribed
         SELECT MAX (POS)
           INTO POS2
           FROM subscribers
          WHERE id = notif_uuid;
      
     IF POS2 IS NULL THEN
      POS2:=0;
     END IF;

         FOR username
            IN (SELECT DISTINCT userid
                  FROM usersubscription
                 WHERE usersubscription.docid = subscription.docid
                       AND usersubscription.notification =
                              notification.notification)
         LOOP
            SELECT COUNT (*)
              INTO CNT
              FROM subscribers
             WHERE id = notif_uuid AND item = username.userid;

            IF CNT = 0
            THEN
               INSERT INTO subscribers (id, pos, item)
                    VALUES (notif_uuid, POS2, username.userid);

               TOTALCNT := TOTALCNT + 1;
               POS2 := POS2 + 1;
            ELSE
               DBMS_OUTPUT.put_line (
                     'WARNING : Username % already subscribed'
                  || username.userid);
            END IF;
         END LOOP;

         POS1 := POS1 + 1;
      END LOOP;

      DELETE FROM usersubscription
            WHERE docid = subscription.docid;
      COMMIT;
   END LOOP;
END;

/
EXECUTE nx_migrate_notif;
DROP PROCEDURE nx_migrate_notif;
DROP FUNCTION new_uuid;

