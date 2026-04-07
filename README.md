<!-- Update with new links and icons to wargames parts -->
<!-- [![Curse Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)]() -->
<!--[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)]() -->

[![Patreon](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/patreon-plural_vector.svg)](https://www.patreon.com/c/WargamesDevelopment)
[![Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.wargames.uk)

# HBM-Space Wargames Edition

This as the name implies is a further fork of the [HBM-Space](https://github.com/JameH2/Hbm-s-Nuclear-Tech-GIT) by [James-H2](https://github.com/JameH2) which is a space exploration addition fork of the current [Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) by [HBM](https://github.com/HbmMods). Please checkout our [Credits Section](#meet-our-team--credits) for the full information.

<br>

![HBM-Space Wargames Edition Banner](https://github.com/Wargames-Development/HBM-Space-WDG-Edition/blob/space-travel-twopointfive/assets/HBM-Space-Banner.png?raw=true)

HBM Space Wargames Edition is a systems-focused fork of HBM’s Nuclear Tech Mod (Space branch), designed to improve behaviour in multiplayer and server environments.

This fork does not add significant new content or progression changes. Instead, it introduces ownership tracking and protection-aware behaviour across explosives, missiles, and automated systems. The goal is to allow high-impact weapons to function in structured environments without bypassing territory rules or causing uncontrolled damage.

Changes also hook into our adaptations of MCHELI-O/R Wargames Edition and yRadar Wargames Edition.

## Features (WDG Edition)

This fork focuses on improving system behaviour, ownership tracking, and protection integration for multiplayer environments.  
For standard gameplay, progression, and item documentation, refer to the official HBM Space wiki: https://nucleartech.wiki/wiki/NTM:_Space

---

### Protection & Territory Integration

- **Explosion Validation**
  Explosions are validated against territory rules before applying damage.

- **Protected Block Damage Handling**
  Block destruction is skipped in protected chunks where it is not permitted.

- **Radiation Control**
  Radiation spread is blocked or limited when entering protected areas.

- **Contamination Control**
  Contamination spread is blocked or restricted in protected territory.

- **EMP Behaviour Integration**
  EMP effects respect protection rules instead of applying globally.

- **Player Damage Filtering**
  Damage from explosives and weapons is filtered through protection rules.

---

### Ownership & Attribution

- **Persistent Ownership Tracking**
  Bombs, explosives, missiles, shells, turrets, and launch systems store owner or faction data.

- **No Anonymous Explosives**
  All explosive sources retain attribution from placement or launch to detonation.

- **End-to-End Context**
  Ownership is preserved across long-range weapons and chained systems.

---

### Weapon & Entity Behaviour

- **Territory-Aware Impacts**
  Missiles, rockets, and shells respect protection rules at their impact location.

- **Faction-Aware Turrets**
  Turrets use ownership and faction relationships for targeting decisions.

- **Projectile Ownership Inheritance**
  Turret and player-fired projectiles inherit ownership context.

---

### Missile System Improvements

- **Bunker-Buster Penetration**
  Missiles can penetrate surfaces before detonation depending on impact conditions.

- **Airburst Detonation**
  Missiles can detonate above ground for wider area coverage.

- **Cluster / Burst Improvements**
  More consistent behaviour for multi-stage and cluster payloads.

- **Improved Drill / Penetration Handling**
  Enhanced logic for deep-impact and drilling-style munitions.

---

### Launch Systems

- **Designator Ownership Tracking**
  Target designators store ownership data for correct attribution.

- **Launch Validation**
  Launch systems check whether a target is valid before firing.

- **Protection Integration**
  Launch behaviour is integrated with territory and protection rules.

## Documentation

### Documentation Coming Soon.

<!--
If there is some documentation then please include this and update the link! The website forum documentation page needs to be produced first...

We now have documentation, it is still early, so not everything might be there, you can check it out [here](https://docs.wargames.uk/<mod>)!
-->

<br>

---

## Support Us!

<!-- Update this once wargames hosting comes out properly to direct to purchase a server! -->

Are you enjoying our mod?
Consider supporting our development!

Instead of asking for donations the **Wargames Development Group** have produced a project called host.wargames.uk (yet to release) Please consider keeping an eye out for when we release support through server hosting!

But for now until that is released, all monetary contributions made via Patreon are being put right back into the development of the mod, our Server and our Hosting Company.

[![Patreon](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/patreon-plural_vector.svg)](https://www.patreon.com/c/WargamesDevelopment)

## Need to get in touch?

<!-- If Discord server or contact lines via email change, update this section here. -->

Our primary way of communicating with the community is through our [Discord Server](https://discord.wargames.uk).
Join our great community today!

Feel free to send an email to dev@wargames.uk if you have any concerns about the development, or if you find dangerous issues or abuse, contact abuse@wargames.uk
Please note that this inbox will not reply to any queries or help about the mod itself, please use the discord server for that instead.

---

## Compiling a current Version

If you are annoyed by our slow releases (since we work on the server's schedule), and you can see we have done work,
feel free to compile it yourself, however it might not work due to incomplete fixes or updates!

<!-- This is a very basic guide to getting the repo setup, this is on purpose, but could be updated if things change or is wanted -->

<details>
<summary>View Detailed Steps:</summary>

1. Enter the source code directory
   1. Navigate to the location where you downloaded the sources. *it should be `C:/Users/%USER%/Downloads`*

   2. Enter the downloaded source tree.

   3. For Win11 Shift Right-Click, and select `Open in terminal` This will open a CMD instance in this location, *if this for some reason is a powershell instance please follow below:*
        1. Open a CMD window (search CMD)

        2. cd to the directory:

        ```cmd
            cd /path/to/project-root/dir/
        ```

<br>

2. Build the mod
    1. Type `gradlew build` and then click enter

    2. Wait for completion

<br>

3. Locate the mod file.
   1. Navigate to the location where you downloaded the sources. *it should be `C:/Users/%USER%/Downloads`*

   2. Enter the downloaded source tree.

   3. Navigate to `build/libs`.

   4. Grab the .jar file from there. *This mod might be unstable due to the state of current development*

</details>

## Contributing

<!-- This is a very basic guide to getting the repo setup, this is on purpose, but could be updated if things change or is wanted -->

Anyone and everyone is welcome to contribute and help out with the project!
However, We hope you have some understanding of modding and therefore are giving basic instructions below

<details>
<summary>View Detailed Steps:</summary>

1. Follow the Step 1 from compiling the latest version above,

2. Setup the workspace
    1. Type `gradlew setupDecompWorkspace` and then click enter

    2. Wait for completion

3. Depending on your editor of choice follow one of the below:

* Intellij Idea:
    1. Generate idea files by running `gradlew idea` in the cmd.

    2. Open the .ipr file in the explorer to intellij Idea.

* Eclipse Users:
    1. Generate eclipse files by running  `gradlew eclipse` in the cmd.

    2. Select the **eclipse** folder as a workspace when opening eclipse.

</details>

### Want to join the Development Team?

We are always looking for people to assist us in our development, as our time is more pushed into the infostructure, hardware and minecraft server.
Therefore if you wish to help out in a more official way then please get in contact with us through our Discord Server. (only if you've previously worked on any other projects)

[![Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.wargames.uk)

## Meet our Team & Credits

<!-- Add Credit to the developers of any used code, models or textures, including links. -->

Another massive thank you to all the contributors and members of the development team.
We wouldn't be where we are now without the support from you all!

All Credits for the content prior is fully credited to [HBM](https://github.com/HbmMods) for creating [HBM's NTM](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT), and [James-H2](https://github.com/JameH2) for [HBM-Space](https://github.com/JameH2/Hbm-s-Nuclear-Tech-GIT). Content will be continued to be sync'd to try and keep it in line with the current space fork. Changes beyond that of what can be found in the HBM-Space fork is original code/adjustments including all content of the Features section having been developed by [Glac](https://github.com/RhysHopkins04) of the [WDG](https://github.com/Wargames-Development).

### Wargames Development Group Team

- [Glac](https://github.com/RhysHopkins04) - Developer
- [Barrack](https://github.com/BateNacon) - Developer
- [Ocean](https://github.com/Oceanseaj) - Advisor
- [Viking](https://github.com/snowboardman91) - Advisor


### Contributors

[![Contributors](https://contrib.rocks/image?repo=Wargames-Development/HBM-Space-WG)](https://github.com/Wargames-Development/HBM-Space-WG/graphs/contributors)

### OLD README:

To view the old README.md that was attached to the main HBM-Space Repository, please view [OLD README](/HBM-README.md).
