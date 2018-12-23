(ns todomvc.react-bootstrap-core
  (:require
   ;[cljsjs.react-bootstrap]
    [reagent.core :as reagent]
    [tupelo.core :as t]
    ) )

;(js/console.log "js/ReactBootstrap=" js/ReactBootstrap)
;
;(def accordion                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Accordion"))))
;(def alert                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Alert"))))
;(def badge                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Badge"))))
;(def breadcrumb                 (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Breadcrumb"))))
;(def breadcrumb-item            (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "BreadcrumbItem"))))
;(def button                     (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Button"))))
;(def button-group               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ButtonGroup"))))
;; (def button-input               (reagent/adapt-react-class (aget js/ReactBootstrap "ButtonInput")))
;(def button-toolbar             (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ButtonToolbar"))))
;(def carousel                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Carousel"))))
;(def carousel-item              (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "CarouselItem"))))
;
;(def checkbox                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Checkbox"))))
;(def clearfix                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Clearfix"))))
;(def close-button               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "CloseButton"))))
;
;(def col                        (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Col"))))
;(def collapse                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Collapse"))))
;; (def collapsible-nav            (reagent/adapt-react-class (aget js/ReactBootstrap "CollapsibleNav")))
;(def control-label              (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ControlLabel"))))
;
;(def dropdown                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Dropdown"))))
;(def dropdown-button            (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "DropdownButton"))))
;(def fade                       (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Fade"))))
;
;(def form                       (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Form"))))
;(def form-control               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "FormControl"))))
;(def form-group                 (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "FormGroup"))))
;
;(def glyphicon                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Glyphicon"))))
;(def grid                       (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Grid"))))
;
;(def help-block                 (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "HelpBlock"))))
;(def image                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Image"))))
;
;(def input-group                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "InputGroup"))))
;; (def input                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Input"))))
;; (def interpolate                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Interpolate"))))
;(def jumbotron                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Jumbotron"))))
;(def label                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Label"))))
;
;(def list-group                 (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ListGroup"))))
;(def list-group-item            (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ListGroupItem"))))
;
;(def media                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Media"))))
;(def menu-item                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "MenuItem"))))
;(def modal                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Modal"))))
;(def modal-body                 (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ModalBody"))))
;(def modal-footer               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ModalFooter"))))
;(def modal-header               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ModalHeader"))))
;(def modal-title                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ModalTitle"))))
;
;(def nav                        (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Nav"))))
;; (def nav-brand                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "NavBrand"))))
;(def nav-dropdown               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "NavDropdown"))))
;(def nav-item                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "NavItem"))))
;(def navbar                     (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Navbar"))))
;(def navbar-brand               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "NavbarBrand"))))
;(def overlay                    (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Overlay"))))
;(def overlay-trigger            (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "OverlayTrigger"))))
;(def page-header                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "PageHeader"))))
;(def page-item                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "PageItem"))))
;(def pager                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Pager"))))
;(def pagination                 (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Pagination"))))
;(def pagination-button          (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "PaginationButton"))))
;
;(def panel                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Panel"))))
;; (def panel-heading              (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Panel.Heading"))))
;(def panel-group                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "PanelGroup"))))
;
;(def popover                    (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Popover"))))
;(def progress-bar               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ProgressBar"))))
;(def radio                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Radio"))))
;
;(def responsive-embed           (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ResponsiveEmbed"))))
;(def row                        (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Row"))))
;(def safe-anchor                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "SafeAnchor"))))
;(def split-button               (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "SplitButton"))))
;
;(def tab                        (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Tab"))))
;(def tab-container              (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "TabContainer"))))
;(def tab-content                (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "TabContent"))))
;(def tab-pane                   (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "TabPane"))))
;
;(def table                      (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Table"))))
;(def tabs                       (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Tabs"))))
;(def thumbnail                  (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Thumbnail"))))
;
;(def toggle-button              (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ToggleButton"))))
;(def toggle-button-group        (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "ToggleButtonGroup"))))
;
;(def tooltip                    (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Tooltip"))))
;(def well                       (reagent/adapt-react-class (t/spyx (aget js/ReactBootstrap "Well"))))
;
;
;

