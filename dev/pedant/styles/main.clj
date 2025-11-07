(ns pedant.styles.main
  (:refer-clojure :exclude [rem])
  (:require [garden.def :as garden]
            [pedant.styles.core :as core]
            [pedant.styles.components.menus :as menus]
            [pedant.styles.elements.forms :as forms]
            [pedant.styles.elements.lists :as lists]
            [pedant.styles.elements.media :as media]
            [pedant.styles.elements.tables :as tables]
            [pedant.styles.elements.typography :as typography]
            [pedant.styles.layout.document :as document]
            [pedant.styles.layout.mini-classes :as mini-classes]
            [pedant.styles.layout.page :as page]
            [pedant.styles.layout.reset :as reset]
            [pedant.styles.layout.structure :as structure]
            [pedant.styles.media.responsive :as responsive]
            [pedant.styles.pages.authentication :as authentication]
            [pedant.styles.pages.authentication :as authentication]
            ))

(garden/defstyles screen

; Layout
;reset/screen
document/screen
;page/screen
;structure/screen
;mini-classes/screen

; Elements
;typography/screen
;forms/screen
;lists/screen
;media/screen
;tables/screen

; Componenents
;menus/screen

; Pages
;authentication/screen

; Media
;responsive/screen

; Fonts
;core/fonts

)
