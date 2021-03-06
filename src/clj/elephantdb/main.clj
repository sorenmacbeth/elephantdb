(ns elephantdb.main
  (:use [elephantdb config hadoop]
        [clojure.java.io :only (resource)]
        hadoop-util.core)
  (:require [elephantdb.log :as log]
            [elephantdb.service :as service]
            [elephantdb.util :as util])
  (:gen-class))

;; # Main Access
;;
;; This namespace is the main access point to the edb code. Running
;; elephantdb.main Boots up the server, and an updater process that
;; watches all domains and trigger an atomic update in the background
;; when some new version appears in domains.

(defn launch-server!
  [global-config local-config]
  (let [{interval :update-interval-s} local-config
        {port :port} global-config
        handler (service/service-handler global-config local-config)
        server  (service/thrift-server handler port)]
    (util/register-shutdown-hook #(do (.shutdown handler)
                                      (.stop server)))
    (log/log-message "Starting updater process...")
    (service/launch-updater! interval handler)
    (log/log-message "Starting ElephantDB server...")
    (.serve server)))

(defn -main
  "Main booting function for all of EDB. Pass in:

  `global-config-hdfs-path`: the path of `global-config.clj`, located
    on ec2

  `local-config-path`: the path to `local-config.clj` on this machine."
  [global-config-hdfs-path local-config-path]
  (log/configure-logging (resource "log4j.properties"))
  (let [local-config  (read-local-config local-config-path)
        global-config (read-global-config global-config-hdfs-path
                                          local-config)]
    (launch-server! global-config local-config)))
