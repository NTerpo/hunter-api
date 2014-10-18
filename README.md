# hunter-api

REST API  to handle open datasets meta-data

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2014 Raw Data Hunter - Nicolas Terpolilli

# Data Hunter - Value Proposition

Find open data easily and quickier than ever

## Explication

---------------------------
$ hunter france population

title: "Campagne 2001 de recensements nationaux - Population par sexe, âge, type de ménage et situation du ménage" 

description: "Ménages, Population par sexe, âge, type de ménage et situation du ménage
Tableau 1 : Données interprétées
Tableau 2 : Données brutes
Tableau 3 : Légende de l'indicateur age (Classe d'âge)
Tableau 4 : Légende de l'indicateur geo (Entité géopolitique (déclarante))
Tableau 5 : Légende de l'indicateur hhtyp (Type de ménage)
Tableau 6 : Légende de l'indicateur sex (Sexe)"

spacial: France

date: 2001

producteur: Eurostat

last-modified: 2014-09-17

created: 2013-09-18


open [link](http://www.data-publica.com/opendata/9980--campagne-2001-de-recensements-nationaux-population-par-sexe-age-type-de-menage-et-situation-du-menage-2001)? (yN)

---------------------------

### Rapidité

Peter Thiel: coeur de la VP = 10x meilleur que les concurrents

=> réponse doit être 10x plus rapide qu'une recherche de jeux de données à la main que ce soit sur les sites classiques ou sur les concurrents

5 minutes -> 30 secondes

4 minutes -> 24 secondes

3 miuntes -> 18 secondes

2 minutes -> 12 secondes

### Simplicité

Power Law / Pareto
=> 20% des datasets représentent 80% des jeux de données cherchés / réutilisés
* commment déterminer quels sont ces 20% de jeux de données?
  * Parser les réutilisations
  * déterminer quels sont les jeux de données les plus important
  * apprendre de ce que recherchent les utilisateurs
* que faire quand quelqu'un cherche un ds non indexé?

## Concurrence

* Quandl
* enigma.io
* Google
* Sites Open Data
* Data Catalog
* OKFN
