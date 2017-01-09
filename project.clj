(defproject sevenorless "0.3.0"
  :description "7 items or less"
  :url "https://7itemsorless.com"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [lib-noir "0.9.9"]
                 [ring/ring-defaults "0.1.5"]
                 [org.postgresql/postgresql "9.4-1206-jdbc42"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [clj-http "1.1.2"]
                 [org.clojure/data.json "0.2.6"]
                 [buddy/buddy-hashers "1.1.0"]
                 [com.drewnoakes/metadata-extractor "2.9.1"]]
  :plugins [[lein-ring "0.9.6"]]
  :ring {:handler sevenorless.handler/app}
  :profiles {
    :uberjar {
      :aot :all}
    :production {
      :ring {
        :open-browser? false
        :stacktraces? false
        :auto-reload? false}}
   :dev {
     :dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                    [ring/ring-mock "0.2.0"]]}})
