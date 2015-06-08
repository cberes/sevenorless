(defproject sevenorless "0.2.0"
  :description "7 items or less"
  :url "http://7itemsorless.today"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.2"]
                 [hiccup "1.0.5"]
                 [lib-noir "0.9.5"]
                 [buddy/buddy-hashers "0.4.1"]
                 [ring-server "0.3.1"]
                 [postgresql/postgresql "9.3-1102.jdbc41"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.2"]
                 [clj-http "1.0.1"]
                 [org.clojure/data.json "0.2.6"]
                 [com.drewnoakes/metadata-extractor "2.8.0"]]
  :plugins [[lein-ring "0.8.12"]
            [lein-rpm "0.0.5"]]
  :ring {:handler sevenorless.handler/app
         :init sevenorless.handler/init
         :destroy sevenorless.handler/destroy}
  :rpm {:name "Name"
        :summary "7itemsorless.com"
        :copyright "Corey A Beres"
        :workarea "target"
        :mappings [{:directory "/usr/local/bin/landfill"
                    :filemode "440"
                    :username "dumper"
                    :groupname "dumpgroup"
                    :preinstall {:scriptfile "script.sh"}
                    :requires ["trash-truck > 1.0"]
                    ;; There are also postinstall, preremove and postremove
                    :sources {:source [{:location "target/classes"}
                                       {:location "src"}]
                              :softlinkSource [{:location "/usr/local/bin/new.sh"
                                                :destination "old.sh"}]}}]}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.3.1"]]}})
