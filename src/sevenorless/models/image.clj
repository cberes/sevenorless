(ns sevenorless.models.image
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.element :refer [image link-to]]
            [noir.io :refer [upload-file]]
            [sevenorless.models.db :as db])
  (:import java.io.File
           java.lang.Math
           java.awt.geom.AffineTransform
           [java.awt.image AffineTransformOp BufferedImage]
           javax.imageio.ImageIO
           com.drew.imaging.ImageMetadataReader
           com.drew.metadata.Metadata
           [com.drew.metadata.exif ExifSubIFDDirectory ExifIFD0Directory]
           com.drew.metadata.jpeg.JpegDirectory))

(def default-image-store-path
  (delay (System/getProperty "image.store.path")))

(defn image-store-path []
  @default-image-store-path)

(defn file-ext [content-type]
  (case content-type
    "image/jpeg" "jpg"
    "image/png" "png"
    "image/gif" "gif"
    nil))

(defn image-file-name [{user-id :_id} content-type]
  (str user-id "_" (System/currentTimeMillis)))

(defn open-file [folder filename]
  (File. (File. folder) filename))

(defn read-metadata [file]
;  (doseq [t (map #(.getTags %) (.getDirectories (ImageMetadataReader/readMetadata file)))]
;    (println t))
  (.getFirstDirectoryOfType (ImageMetadataReader/readMetadata file) ExifIFD0Directory))

(defn get-orientation [metadata]
  (if (.containsTag metadata ExifIFD0Directory/TAG_ORIENTATION) (.getInt metadata ExifIFD0Directory/TAG_ORIENTATION) 0))

(defn to-op [t]
  (AffineTransformOp. t AffineTransformOp/TYPE_BILINEAR))

(defn flip-x [height width]
  (let [t (AffineTransform.)]
    (.scale t -1.0 1.0)
    (.translate t (- width) 0)
    (to-op t)))

(defn rotate-pi [height width]
  (let [t (AffineTransform.)]
    (.translate t width height)
    (.rotate t Math/PI)
    (to-op t)))

(defn flip-y [height width]
  (let [t (AffineTransform.)]
    (.scale t 1.0 -1.0)
    (.translate t 0 (- height))
    (to-op t)))

(defn flip-x-rotate-pi2 [height width]
  (let [t (AffineTransform.)]
    (.rotate t (/ Math/PI -2))
    (.scale t -1.0 1.0)
    (to-op t)))

(defn translate-and-rotate-pi2 [height width]
  (let [t (AffineTransform.)]
    (.translate t height 0)
    (.rotate t (/ Math/PI 2))
    (to-op t)))

(defn flip-and-rotate-pi2 [height width]
  (let [t (AffineTransform.)]
    (.scale t -1.0 1.0)
    (.translate t (- height) 0)
    (.translate t 0 width)
    (.rotate t (* 3 (/ Math/PI 2)))
    (to-op t)))

(defn rotate-pi2 [height width]
  (let [t (AffineTransform.)]
    (.translate t 0 width)
    (.rotate t (* 3 (/ Math/PI 2)))
    (to-op t)))

(defn build-transformation [metadata]
  (case (if metadata (get-orientation metadata) 0)
    2 {:transform flip-x :swap false}
    3 {:transform rotate-pi :swap false}
    4 {:transform flip-y :swap false}
    5 {:transform flip-x-rotate-pi2 :swap true}
    6 {:transform translate-and-rotate-pi2 :swap true}
    7 {:transform flip-and-rotate-pi2 :swap true}
    8 {:transform rotate-pi2 :swap true}
    nil))

(defn height [img swap]
  (if swap (.getWidth img) (.getHeight img)))

(defn width [img swap]
  (if swap (.getHeight img) (.getWidth img)))

(defn rotate-image [folder filename type]
  (let [file (open-file folder filename)
        src (ImageIO/read file)
        metadata (read-metadata file)
        t (build-transformation metadata)
        transform (:transform t)
        swap (:swap t)]
    (when transform
      (ImageIO/write (.filter
                       (transform (.getHeight src) (.getWidth src))
                       (ImageIO/read file)
                       (BufferedImage. (width src swap) (height src swap) (.getType src)))
                     type file))))

(defn save-image [{:keys [content-type filename] :as file} user]
  (when-not (empty? filename)
    (let [folder (image-store-path) new-file-name (image-file-name user content-type) ext (file-ext content-type) filename (str new-file-name "." ext)]
      (upload-file folder (assoc file :filename filename) :create-path? true)
      (rotate-image folder filename ext)
      (:_id (first (db/add-image {:user_id (:_id user) :path new-file-name :ext ext}))))))

(defn delete-image [id]
  (when-not (nil? id)
    (when-let [img (db/get-image id)]
      (io/delete-file (str (image-store-path) File/separator (:path img)) true)
      (db/delete-image id))))
