
# Le contexte

## Déploiement Nuxeo

Plusieurs projets CMS/GED basés sur Nuxxeo sont en cours de réalisation ou en phase de déploiement :

 - DSAF
 - DISIC
 - CADA
 - ...

## Besoins remontés à la DSI

Plusieurs besoins similaires de CMS et/ou GED sont identifiés pour les différents services.

La mise en oeuvre en mode projet pour chaque demande n'est pas envisageable et ne semble pas optimisée.

## Le projet DSAF

Le projet DSAF, dont la phase de réalisation n'a pas encore commencé, a besoin de plusieurs briques fonctionnelles (CMS, Photothèque, Groupware) qui d'un point de vue global pourrait correspondre aux besoins génériques remontés par les différents services.

## Problématique

Dans ce contexte, la DSI souhaite :

 - packager une offre standard

   => pouvoir accélérer la mise en oeuvre des projets "simple" en proposant une offre standard capable de répondre à la majorité des besoins

 - pouvoir garantir la cohérence et maintenabilité en évitant le foisonnement de solutions différentes et hétérogènes.


# Approches possible

Ce chapitre étudie les différentes approches possibles pour traiter la problématique identifiée.

## Pre-requis : définir le socle

Quelque soit la/les approches choisies, il sera nécessaire de définir le contenu du socle commun.

Ce socle commun défini :

 - les différents composants techniques utilisés

   - version de Nuxeo

   - distribution Nuxeo (CAP, DM, SC ...)

   - les composants additionnels (spécifiques SPM ou non)

   - les configurations communes (Theme, LDAP, Types de documents, Modèles de workflows ...)

 - les différentes fonctionnalités proposées

   - Collaboration

   - Publication Web

   - ...

## Approche multi-tenants : type SAAS

### Principe

Le principe est d'héberger de manière centralisée une seule instance applicative et de mettre à disposition dans cette unique instance différents services (site web, espace de collaboration ...).

Afin de garantir l'étanchéité des données et un minimum de personnalisation, il est possible d'utiliser :

 - la gestion des droits ( simples ACL ou Security policy ) pour réaliser l'isolation des données

 - la configuration locale pour permettre un peu de personnalisation (theme, types de documents ...)

### Avantages

 - Maintenance facile : une seule application à héberger et à maintenir

 - Mise en oeuvre facile et économique : pour chaque projet la mise à disposition des services est rapide

 - Cohérence : aucun risque de divergence des socles techniques

### Contraintes

 - Personnalisation limitée :

   Les possibilités de personnalisation sont limitées car au final tout tourne dans la même application et la configuration locale permet seulement d'activer ou de cacher des configurations.
   En conséquence cette approche peut poser des soucis :

   - pour gérer beaucoup de types de documents personnalisés (multitude de tables SQL dans la même DB)

   - pour personnaliser certaines fonction (ex: la gestion des vocabulaires per-tenant n'est pas dispobible pour l'instant au niveau UI).

 - Montée en charge et disponibilité :

   Le fait de concentrer sur une seule application plusieurs projets augmente le volume total de stockage, le nombre de sollicitations ainsi que les contraintes de disponibilité. Il est donc nécessaire de prévoir une architecture adéquate permettant de garantir la montée en charge et la redondance.

 - Trains de mise à jour

   Toute évolution du socle doit être programmée à l'avance et validée en profondeur.
   En conséquences, les mises à jours sont plus longues.

 - La mise en place d'un archivage automatique est nécessaire

## Approche multi-instances : type PAAS

### Principe

Le principe est d'instancier "à la demande" un instance Nuxeo+Socle SPM pour chaque projet.
Cette approche se base sur l'utilisation d'un système de virtualisation permettant de facilement pouvoir provisionner une nouvelle VM.

Dans cette logique le service CMS+GED est rendu par une ou plusieurs instance dédiées.
Selon les besoins de chaque projet, il est possible de prévoie :

 - une solution "light" : Nuxeo = SGBD sur la même VM

 - une solution HA : 2 VM Nuxeo + 1 VM SGBD

 - ...

Chaque projet disposant de "son(ses) instance(s)", chaque projet peut réaliser de la configuration avancée de manière sécurisée via Nuxeo Studio :

 - types de documents

 - types de workflows

 - formulaires

 - traitements automation

 - theme

 - ...


### Avantages

 - paramétrage fonctionnel facile et souple

 - standardisation des instances (simplification de l'exploitation)

 - moins de contraintes pour la disponibilité, les mises à jours et la montée en charge
   (chaque instance à les contraintes du projet associé)

 - chaque projet peut faire les mises à jours à son rythme

### Contraintes

 - Nécessite une industrialisation de la création et du monitoring d'une instance
   ( dépend de la maturité de la virtualisation chez SPM)

La mise en place de processus d'automatisation de création et de monitoring des VM doit permettre de réduire de manière significative le surcoût de l'approche multi-instance par rapport à l'approche d'instance mutualisée.

## Approche socle technique : SDK

Pour tous les projets nécessitant des développements spécifiques importants, les deux premières approches ne sont pas applicables.

Il est cependant possible de mutualiser le socle technique afin :

 - de garantir la cohérence

 - de permettre l'enrichissement du socle commun

Cette approche est donc complémentaire des deux autres.


## Recommandations Nuxeo

De notre expérience l'approche SAAS n'est adaptée que quand le besoin de personnalisation est très faible ou inexistant.
A partir du moment où les "clients" du service ont des besoins légèrement différents, il devient très difficile de synchroniser les différentes demandes avec des cycles de mise à jour commun.

Etant donné les natures différentes des "clients" du service CMS+GED SPM, il semble très difficile de pouvoir partir sur une approche SAAS, surtout s'il n'y a pas de lien hiérarchique permettant d'imposer la solution et les contraintes associées.

Notre recommandation est donc de partir sur une approche PAAS qui a l'avantage de permettre plus de flexibilité sur tous les aspects :

 - cycle de mise à jour
 - fonctionnalités disponibles
 - besoins de configuration

Cette approche devrait s'accompagner de travaux permettant d'automatiser les taches de déploiement et de provisionning.


# Socle commun, DSAF, Nuxeo et approche de generisation

## Opportunités de convergence

Au vu des besoins intégrés au socle commun, des synergies sont possibles avec le Projet DSAF et/ou avec la plate-forme Nuxeo.

Le principe de base est de dire que certains des modules du socle commun cible pourraient être :

 - la base de certains modules de DSAF

 - basé sur des modules génériques de la plate-forme Nuxeo

Ce chapitre a pour but d'étudier les enjeux et impacts d'une telle approche.

## Différences entre développements socle et développement projet

Il est clair que certaines des fonctions nécessaires au socle commun correspondent à des besoins du projet DSAF.

Il est cependant important d'être conscient que l'effort de réalisation est supérieur quand il s'agit de réaliser une brique générique réutilisable.
Le surcoût est transverse et touche chaque phase de la mise en oeuvre :

 - la charge de spécification est supérieure
   car il ne suffit pas de définir les cas d'utilisation d'un projet mais de prévoir les différents cas d'utilisation possibles

 - la charge de développement est supérieure
   le développement d'un module générique impose en général de prévoir des points de configuration ou de contribution ce qui nécessite plus de code
   la qualité du code doit aussi être supérieure car les enjeux de maintenance sont plus importants

 - la charge de test est supérieure
   le nombre de cas d'utilisation à tester est supérieur
   le besoin d'assurance qualité est supérieur (un bug n'impacte pas qu'une application mais plusieurs)

Selon les cas et les contraintes, le ratio entre un développement spécifique et un développement générique est généralement compris entre 1,5 et 3.
Le cas d'intégration du module à la plate-forme Nuxeo est sans doute celui où le ratio est le plus important car les contraintes de compatibilité et de maintenance sont maximales.

En conséquence, la réalisation de développements génériques (générique SPM ou générique Nuxeo) dans le cadre du projet DSAF a forcement un impact sur les délais et les charges de développement.

## Impact sur le projet DSAF

### Charge

Comme exposé précédemment, la réalisation de développements générique à la place de développement spécifiques augmente la charge initiale de réalisation.

Cependant, comme expliqué plus bas, cette charge supplémentaire court terme peut être compensé par les gains en maintenance et en évolutions.

C'est un surcoût initial pour un gain à moyen terme.

### Planning

La charge de développement et de test augmentant, le planning peut être impacté.

Une approche possible pourrait être de réaliser le travail en plusieurs étapes :

 - réalisation des modules DSAF

 - extensions des modules DSAF pour les rendre génériques

 - ré-alignement de DSAF sur les modules générique

Cette approche peut sembler séduisante, mais se révèle souvent périlleuse dans sa mise en pratique :

 - gestion en parallèle de plusieurs branches
   (ex: branches DSAF en maintenance et generic en dev)

 - la réintégration est souvent remise à plus tard et le résultat est d'avoir 2 bases de codes à maintenir

Le plus sage est donc de prévoir de mener les 2 chantiers de front, même si cela peut signifier avoir plus de ressources travaillant conjointement.

D'un point de vue technique, mener les 2 chantiers de front a aussi plusieurs avantage :

 - les développements génériques sont vite confrontés à la réalité projet

 - le développement est piloté par de vrais utilisateurs

 - le projet bénéficie d'une qualité supérieure

#### Gains

Quand elle est mise en oeuvre correctement, l'approche de mutualisation et de générisation du code est gagnante pour tous les participants.

*Pour le projet DSAF*

 - qualité accrue des développements
 - meilleurs possibilités d'évolution et d'extension
 - maintenance facilité car le volume de code spécifique est réduit

*Pour la DSI SPM*

 - mutualisation des efforts de développements
 - mise en oeuvre rapide du socle générique dans un projet
 - maintenance simplifié via la réintégration de certains modules dans la plate-forme

*Pour Nuxeo*

 - extension de la plate-forme dirigée par de vrai besoins utilisateur

## Tâches nécessaires à l'approche de mutualisation

Ce paragraphe a pour but de lister les travaux d'infrastructure nécessaires à la mise en oeuvre, dans de bonnes conditions, d'un socle SPM/DSAF/Nuxeo.

### Alignement des besoins (Gap Analysis)

Afin de mutualiser les développements entre SPM/DSAF/Nuxeo il est nécessaire pour chaque besoin exprimé de définir :

 - ce qui pourrait avoir sa place dans la plate-forme Nuxeo

 - ce qui a sa place dans la partie spécifique du Socle SPM

 - ce qui doit rester en configuration ou extension spécifique DSAF

Le chapitre suivant propose une première analyse d'alignement, mais devra clairement être précisée et confrontée aux besoins réels.

### Assurance Qualité

La maintenance d'un socle commmun SPM nécessite un effort particulier sur l'assurance qualité.
Cela est nécessaire :

 - pour garantir que les projets utilisent un socle sain et testé

 - pour garantir que les évolutions du socle apportées par chaque projet ne le compromettent pas

Notre recommandation en la matière est de mettre en place un environnement d'intégration continue complet.
La plate-forme Nuxeo étant déjà intégrée avec les standards du monde Java en utilisant des produits OpenSource, la mise en oeuvre d'une telle chaine ne représente pas un gros effort.

Typiquement :

 - mise en place d'un repository Maven interne (type Nexus)

 - mise en place d'une chaine d'intégration (type Jenkins)

 - mise en place de test unitaires (Nuxeo fournis le Framework pour cela)

 - mise en place de test fonctionnel (Nuxeo fournis le Framework pour cela)

 - mise en place de test de performance (Nuxeo fournis le Framework pour cela)

La mise en oeuvre du processus complet d'assurance qualité est d'autant moins coûteuse qu'elle est mise en place dès le début.

### Environnement d'intégration

Il est important de prévoir la mise en place d'un environnement d'intégration permettant de tester facilement les différentes instanciation du socle avec un environnement cible représentatif (LDAP, SSO, SGBD ...).

### Modèle de packaging

#### Distribution de base Nuxeo

La première étape est de définir la partie de la plate-forme Nuxeo utilisée comme base.
En première approche, cela pourrait être DM + SC, mais il est possible qu'il soit intéressant d'intégrer des plugins additionnels.

Cette distribution de base devrait idéalement être dispo sous au moins 2 formes :

 - artifact Maven : pour le build, les tests ...

 - package Marketplace : pour l'installation facile en production

#### Socle

Le socle regroupe les composants complémentaires spécifiques SPM.
Ce socle doit aussi être disponible sous les 2 formes :

 - artifact Maven

 - package Marketplace

Selon les besoins il pourrait y avoir plusieurs déclinaisons du socle :

 - CMS uniquement

 - GED uniquement

 - CMD + GED

 - ...

#### Projets

Chaque projet sera doit constitué au final :

 - de la distribution de base Nuxeo
 - d'un package du socle
 - de bundles complémentaires

Afin de faciliter les tests et l'installation, il est possible de tout packager sous forme d'un package Marketplace.



# Contenu du socle commun

Le périmètre actuellement défini comme cible pour le cas d'utilisation de la plate-forme Nuxeo recouvre les domains CMS (WCM) et GED (Travail collaboratif).
Ce chapitre propose une première démarche d'alignement entre ces besoins et l'existant dans la plate-forme Nuxeo.

L'alignement vis à vis du projet DSAF devra faire l'objet d'une discussion ultérieure dans la mesure où il sera nécessaire de définir précisément les besoins de DSAF et ceux du socle générique.

Dans tous les cas la démarche globale reste la même :

 - définir les besoins génériques du socle

 - regarder ce qui peut être récupéré dans le cadre DSAF

 - regarder ce qui existe ou peut être étendu dans la plate-forme Nuxeo

## CMS et WCM

### Usine de site web

C'est le besoin de base de type WCM :

 - pouvoir définir des modèles de page
     - modèles de mise en page
     - templates de rendu
 - pouvoir définir une arborescence de consultation
 - disposer d'un modèle de versionning et de publication adapté
 - gestion des liens et des ressources (galerie photo par exemple)

La plate-forme Nuxeo ne dispose pas en standard d'un module dédié à la gestion de site web, ou du moins pas de d'usine a site générique.

Cependant la majorité des fonctions d'infrastructure sont déjà disponibles dans la plate-forme.

**Modèle de publication**

Plusieurs modèles de publications sont disponibles, mais le principal reste celui exposé par le PublisherService.

Celui ci a plusieurs avantages :

 - il existe en standard
 - son comportement est très pluggable et configurable
 - il sait publier en local ou en distant
 - l'utilisation du modèle de proxy permet une gestion native des contraintes de sécurité
 - il est déjà interfacé avec un modèle de validation des publication

Cependant, en l'état il présente certaines limitations :

 - il n'est pas prévu pour la publication en masse d'arborescence
   (selon les cas d'utilisation cela peut ou non être problématique)
 - le modèle de versionning d'arbre si nécessaire n'est pas inclus

Ce dernier point est un des plus important.

La logique de publication de site web dépend en effet beaucoup du modèle choisi pour :

 - valider une arborescence de page

 - pouvoir revenir en arrière

Tout dépend donc du modèle souhaité.

**Modèle de mise en page et de rendu**

Sur cet aspect aussi, la plate-forme Nuxeo dispose de nombreuse solutions.

*Moteur de thèmes*

Le moteur de thèmes peut être utilisé pour définir des modèles de mise en page.
C'est cette solution qui est utilisé dans le module website par défaut de Nuxeo.

*OpenSocial*

Le support natif des gadgets OpenSocial permet de réaliser facilement des sites de type "portail" dans lesquels il est facile de choisir les différents contenus à agréger sur chaque page.

Le modèle actuel permet déjà de disposer :

 - d'un conteneur d'affichage GWT permettant à l'utilsiateur de le personnaliser
 - d'un conteneur JS permettant l'intégration rapide dans une page HTML
 - d'un modèle de Layout basé sur YUI (http://developer.yahoo.com/yui/layout/) et pouvant être persisté dans des documents

Ce modèle a été utilisé dans plusieurs projet de portail intranet.

*WebEngine*

Le modèle natif WebEngine permet de définir des templates dans une arborescence de skins supportant l'héritage.
Ce modèle permet de mettre en place très vite un moteur de site en gérant la navigation et le rendu.

Ce modèle est utilisé par de nombreux projets pour réaliser une vue Web dédié sur la base de contenu Nuxeo.

*Renditions et Document Template*

Les travaux en cours dans le cadre de version 5.6 de Nuxeo étendent la notion de rendition et permettent d'associer à tout document un template de rendu lui même stocké dans un document.
ce modèle permet de compléter le système de skins de WebEngine pour avoir la possibilité de définir des modèles rendu au moyen de templates gérés sous forme de donnée et non sous forme de configuration.
Cette fonctionnalité ouvre donc la porte à l'édition dynamique de template et donc à la personnalisation dynamique des modèles de rendu d'un document ou d'une page.

**Architecture Cible potentielle dans Nuxeo**

En se basant sur les briques existante, il est possible et même assez logique de définir dans la plate-forme Nuxeo un modèle d'usine a site web qui soit générique et réutilisable.

Cela permettait entre autre :

 - de réduire le travail de mise en oeuvre aussi bien pour SPM que DSAF
 - de réduire les problématiques de maintenance et de montée de version

Un tel module pourrait se baser sur les principes suivants :

 - un module WebEngine consituant le squelette du site
   - modèle de gestion d'url
   - gestion technique de la navigation
   - définition des templates de base

 - utilisation de OpenSocial pour remplir de portlets configurables certains slots des templates de base
   - News, A la une ...
   - Enquêtes sondages, ...

 - utilisation des renditions pour permettre la personnalisation du rendu document par document en cas de besoin

 - structure de plan de classement via du contenu
   - section simples, ordonée ou "smart section" (recherche enregistrées)
   - utilisation simple de tagging pour définir le versionning / snapshoting

### Forum, Commentaires, Sondages, Feeds ...

La plate-forme Nuxeo comprend déjà des modules fournissant :

 - un forum simple
 - un système de commentaires imbriqué
 - un système d'annotation
 - un module de sondage simple (addon nuxeo-platform-poll)
 - un moteur de syndication

La première étape serait donc de bien regarder le delta entre les demandes SPM/DSAF et l'existant Nuxeo.
En effet, la solution la plus simple serait de directement enrichir les modules standards Nuxeo pour ne garder dans le socle SPM ou DSAF que de la configuration et éventuellement de l'affichage.

### Outils de collaboration

Pour la partie collaborative, il semble intéressant de s'appuyer le plus possible sur les briques existantes dans la plate-forme Nuxeo.

La version 5.5 et la future 5.6 ajoutent de nombreuses fonctions collaboratives qui devraient être confrontées aux besoins exprimés :

 - Espaces collaboratifs via le module Social Collab
 - Annotation et système de diff (5.6)
 - Workflow via Content Routing
 - Tableau de bord de "mes communautés"

En fonction des écarts observés, il sera possible d'ajouter à la plate-forme Nuxeo ce qui manque.

### Photothèque

Nuxeo ne dispose pas à proprement parler d'une photothèque, mais en se basant sur les fonctions du PictureBook ou de la vue DAM, il est facile de mettre en place une fonctionnalité de type photothèque.

### Autre fonctions

Certaines fonctions n'existent aujourd'hui pas du tout dans Nuxeo :

 - Agenda simple
 - Mailing list

A discuter en fonction des spécifications détaillées, mais certains modules, comme par exemple l'agenda, font partie des demandes en cours sur la plate-forme et il y a donc potentiellement matière à mutualiser les efforts.

### Modules transverses

Indépendamment des aspects fonctionnels, le socle commun peut aussi avoir intérêt à intégrer des modules techniques liés à l'environnement technique du SPM :

 - Interfaçage LDAP

 - Outils de monitoring

 - Achivage automatique

 - Intégration Exalead

# Plan d'action proposé

## Choix de l'approche souhaité

La première étape est de se positionner sur l'approche cible désirée.

La recommandation de Nuxeo est d'utiliser une approche PAAS car c'est celle qui semble la plus souple et la plus puissante.

## Analyse d'écart sur le projet DSAF

Le principe est d'organiser rapidement des workshops entre :

 - des membres de l'équipe MOE DSAF (Sword sans doute)
 - des membres de l'équipe DSI SPM
 - des membres de l'équipe Nuxeo

Ces workshops auront pour but de définir les spécifications du découpage en modules génériques :

 - ce qui peut être intégré dans la plate-forme Nuxeo (en direct ou en addon)
 - ce qui peut être mis dans le socle commun SPM
 - ce qui reste spécifique au projet DSAF

NB : il est probable qu'une phase préalable soit une discussion avec la MOA DSAF.

## Lancement des développements

En fonction du lotissement DSAF, les développements devront idéalement être lancés en parallèle entre DSAF/SPM/Nuxeo.

## Montage de la plate-forme technique

Ce chantier peut être commencé dès la fin des workshops et a pour but de monter :

 - l'architecture de la plate-forme de production
 - la plate-forme d'intégration continue et de QA

Cette étape doit impliquer idéalement tous les intervenants :

 - MOE DSAF
 - DSI SPM
 - Nuxeo
