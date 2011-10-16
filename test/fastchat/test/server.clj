(ns fastchat.test.server
  (:import [org.webbitserver WebSocketConnection]) 
  (:use [clojure.data.json :only ( json-str read-json )] ) 
  (:use [fastchat.server])
  (:use [clojure.test]))

    (defn mkconn [msgs user]
     (proxy [WebSocketConnection] []
      (send [s] 
       (swap! msgs 
        (fn [msgs] (assoc msgs user (conj (get msgs user) (read-json s)))))
       this))) 

    (deftest wserver
     (let [ channels (mkchannels) 
            users    (atom {}) 
            room  "test"
            user1 "diogok"
            user2 "gislene"
            user3 "girlaine"
            user4 "other"
            msgs  (atom {user1 [] user2 [] user3 [] user4 []})
            conn1 (mkconn msgs user1) 
            conn2 (mkconn msgs user2) 
            conn3 (mkconn msgs user3)
            conn4 (mkconn msgs user4) ]
       (connect channels users room user1 conn1) 
       (Thread/sleep 250) 
       (connect channels users room user2 conn2) 
       (Thread/sleep 250) 
       (connect channels users room user3 conn3) 
       (Thread/sleep 250) 
       (connect channels users "other-room" user4 conn4) 
       (Thread/sleep 250) 
       (is (= (list "Hello, diogok." "Hello, gislene." "Hello, girlaine.") (map :message (get @msgs "diogok") ))) 
       (is (= (list "Hello, gislene." "Hello, girlaine.") (map :message (get @msgs "gislene") ))) 
       (is (= (list "Hello, girlaine.") (map :message (get @msgs "girlaine") ))) 
       (is (= (list "Hello, other.") (map :message (get @msgs "other") ))) 
       (swap! msgs (fn [m] {user1 [] user2 [] user3 [] user4 []})) 
       (post channels users conn1 "Hello, friends!") 
       (Thread/sleep 250) 
       (post channels users conn1 "@gislene love you") 
       (Thread/sleep 250) 
       (is (= (list "Hello, friends!" "@gislene love you") (map :message (get @msgs "diogok")))) 
       (is (= (list "Hello, friends!" "@gislene love you") (map :message (get @msgs "gislene") ))) 
       (is (= (list "Hello, friends!") (map :message (get @msgs "girlaine") ))) 
       (is (= (list) (map :message (get @msgs "other") ))) 
       (is (= (list "diogok" "diogok") (map :from (get @msgs "gislene"))))
       (leave channels users conn2) 
       (Thread/sleep 250) 
       (post channels users conn1 "Yoh!") 
       (Thread/sleep 250) 
       (is (= (list "Hello, friends!" "@gislene love you" "Bye, gislene." "Yoh!") (map :message (get @msgs "diogok")))) 
       (is (= (list "Hello, friends!" "@gislene love you") (map :message (get @msgs "gislene") ))) 
       (is (= (list "Hello, friends!" "Bye, gislene." "Yoh!") (map :message (get @msgs "girlaine") )))
       (command channels users conn1 {:command "users"}) 
       (Thread/sleep 250) 
       (is (= (list "girlaine" "diogok") (:users (peek (get @msgs "diogok"))))) 
      )) 
