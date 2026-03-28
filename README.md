rapport

Ce fichier .md fait office de mini rapport pour comprendre le fonctionnement de l'app PowerHome

- Base de données
  La base de données a une architecture simple et n'a nécessité que desn requêtes simples. Elle est constituée de 5 tables que sont les suivantes :

  > user : comptes résidents (email, password, token de session, eco_coins, langue)
  > habitat : logement d'un résident (étage, surface , lié à un user)
  > appliance : équipements électroménagers d'un habitat (nom, référence, wattage)
  > slot : créneaux horaires de la résidence avec leur taux d'occupation (qui va de 0% à 100%)
  > commitment : engagements d'un résident sur un créneau + variation d'éco-coins associée

- Serveur PHP
  Chaque fichier PHP a un endpoint. Tous lisent les paramètres en GET, vérifient le token si besoin, et répondent en JSON
  - db.php : fichier de base qui assure la connexion MySQL (inclus par tt les autres fichiers avec require)
  - login.php : Vérifie email+password, génère un token, le retourne avec les infos user
  - Register.php : Crée un compte (vérifie que l'email n'existe pas déjà et erreur 409 si doublon)
  - addHabitat.php : Crée un habitat relié à un user (utilisé à l'inscription)
  - getHabitats.php : Retourne tous les habitats de la résident avec leurs appareils
  - getMyHabitat.php : Retourne l'habitat de l'utilisateur connecté (grâce à token) + le total wattage
  - addAppliance.php : Ajoute un appareil à un habitat
  - deleteAppliance.php : Supprime un appareil par son id
  - updateHabitat.php : Modifie l'étage et la surface d'un habitat
  - updateProfile.php : Change le mot de passe (vérifie l'ancien d'abord pour question de sécurité)
  - getSlots.php : Retourne les créneaux d'un mois + indique si l'user a déjà un engagement
  - commitSlot.php : Enregistre un engagement et calcule le bonus/malus en éco-coins
- Application Android
  Dans les fichiers qui ont été créé, il y a 3 types de fichiers que nous avons créé :

  > Les premiers fichiers sont juste des fichiers simples .java, ce sont les deux fichiers suivants :
  - Habitat.java --> Classe représentant un habitat (id, étage, surface, liste d'appareils)
  - Appliance.java --> Classe représentant un appareil. Contient getIconRes() qui choisit l'icône selon le nom de l'appareil

  > Ensuite, il y a les fichiers Activités
  - BaseActivity.java --> Classe mère de toutes les activités. Applique la langue choisie au démarrage
  - LocaleHelper.java --> Utilisé pour sauvegarder et appliquer la langue (fr/en/ar)
  - SplashActivity.java --> Écran de démarrage qui redirige vers Login Activity
  - LoginActivity.java --> Formulaire de connexion. Appelle login.php, sauvegarde le token et redirige vers HabitatActivity
  - RegisterActivity.java --> Inscription pour récupérer des infos personnelles
  - ForgotPasswordActivity.java --> Écran "mot de passe oublié"
  - HabitatActivity.java --> Activité principale post-connexion. Contient la Navigation Drawer (menu latéral) et charge les Fragments selon l'item sélectionné

  > Justement le dernier type de fichiers que l'on a dans notre projet Android ce sont les fragments
  - HabitatsFragment.java --> Liste tous les habitats de la résidence avec leurs appareils (et qd on clique sur son habitat on peut ajouter un équipement directement dessus)
  - HabitatAdapter.java --> Adaptateur custom pour la ListView des habitats. Affiche nom, étage, nb d'appareils et icônes dynamiques
  - MonHabitatFragment.java --> Vue personnelle de l'habitat connecté. Permet d'ajouter/supprimer des appareils et de modifier l'habitat (c'est dynamique)
  - MesRequetesFragment.java --> Calendrier mensuel coloré (vert/orange/rouge selon le taux d'occupation) et qd on clique sur un jour on a la liste des créneaux, la possibilité de s'engager et en fonction ça impacte nos éco-coins
  - ParametresFragment.java --> Affiche le profil et on peut changer son mot de passe et la langue de l'application
