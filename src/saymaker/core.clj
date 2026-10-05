(ns saymaker.core
  "Generate images and video on SayMaker (https://saymaker.ai) from Clojure.

    (require '[saymaker.core :as sm])
    (sm/generate \"a paper boat at sunrise, soft fog, 35mm film\")
    ;; => \"https://cdn.saymaker.ai/uploads/generations/image/....webp\"
    (sm/generate-video \"a fox trotting through fresh snow\" {:duration 6})

  One API key runs the whole shelf on your own credits: Veo 3.1, Kling 3.0,
  Seedance 2.0, MiniMax H3, Nano Banana 2, GPT Image 2.5, Seedream 5.0 and
  more. Pass :api-key or set SAYMAKER_API_KEY; create a key at
  https://saymaker.ai/settings/apikeys.

  Failures throw ex-info whose data carries :reason, one of :auth, :quota,
  :plan, :busy, :rejected, :timeout or :other."
  (:import (io.github.hanshs474.saymaker SayMaker SayMaker$ImageOptions SayMaker$VideoOptions SayMakerException)
           (java.time Duration)))

(defn- client ^SayMaker [{:keys [api-key]}]
  (SayMaker. ^String (or api-key (System/getenv "SAYMAKER_API_KEY") "")))

(defn- rethrow [^SayMakerException e]
  (throw (ex-info (.getMessage e) {:reason (keyword (.toLowerCase (str (.kind e))))} e)))

(defn generate
  "Generates one image, or edits :image-url, and returns its URL.

  opts: :model, :image-url (a public http(s) url; makes the call an edit),
  :aspect-ratio (\"1:1\" \"16:9\" \"9:16\" \"4:3\" \"3:4\"), :api-key, :timeout-ms.
  For an edit, the prompt names the change and what must stay."
  ([prompt] (generate prompt {}))
  ([prompt {:keys [model image-url aspect-ratio timeout-ms] :as opts}]
   (let [^SayMaker$ImageOptions o (SayMaker/image)]
     (when model (.model o model))
     (when image-url (.imageUrl o image-url))
     (when aspect-ratio (.aspectRatio o aspect-ratio))
     (when timeout-ms (.timeout o (Duration/ofMillis timeout-ms)))
     (try (.generate (client opts) prompt o)
          (catch SayMakerException e (rethrow e))))))

(defn generate-video
  "Generates one clip, or animates :image-url as the first frame, and returns its URL.

  opts: :model (default \"minimax-h3-fast\", the one a free account can run),
  :image-url, :duration (seconds), :resolution (\"480p\" \"720p\" \"1080p\"),
  :aspect-ratio (default \"16:9\"), :sound (default true), :api-key, :timeout-ms."
  ([prompt] (generate-video prompt {}))
  ([prompt {:keys [model image-url duration resolution aspect-ratio sound timeout-ms] :as opts}]
   (let [^SayMaker$VideoOptions o (SayMaker/video)]
     (when model (.model o model))
     (when image-url (.imageUrl o image-url))
     (when duration (.duration o (int duration)))
     (when resolution (.resolution o resolution))
     (when aspect-ratio (.aspectRatio o aspect-ratio))
     (when (some? sound) (.sound o (boolean sound)))
     (when timeout-ms (.timeout o (Duration/ofMillis timeout-ms)))
     (try (.generateVideo (client opts) prompt o)
          (catch SayMakerException e (rethrow e))))))
