# Mod Recipe Book

A progression-based recipe book with items from mods. Discover items, unlock recipes, and learn mods as you play. Every recipe opens in a detailed layout that shows exactly how it is made, including recipes made in machines from mods.

Recipe layouts come from [JEI](https://github.com/mezz/JustEnoughItems).

## Features

### Learn mods by playing

- **Recipes unlock as you explore.** Pick up the ingredients and the recipe appears in the book. Finding an item also shows you how to craft it.
- **Find the station first.** A recipe unlocks once you also have what it is made on: a crafting table, a furnace, or a machine from a mod.
- **Your rules.** Unlock recipes after one ingredient or after all of them, with or without the crafting method.

### Categories your way

- **Auto categories.** Every recipe from every mod is sorted into six clear tabs: Blocks, Functional Blocks, Equipment & Utilities, Food & Drinks, Ingredients, and Redstone. No setup needed, and you can reorder the tabs.
- **Custom categories.** Want more control? Build your own tabs from any mix of mods and switch to them in the settings.

### A recipe book that does much more

- **Replaces the vanilla book.** Works in the inventory, crafting table, and furnaces, and adds a recipe book to the brewing stand.
- **Any recipe type.** Shows recipes from any mod, not just crafting and smelting, each in its own detailed layout.
- **Potions and fluids.** Potion recipes are included. Fluid recipes appear in recipe layouts.
- **Bookmarks.** Keep the items you need close at hand.

### Quick to use

- **Left click** a recipe to place it in the grid, **Shift + left click** to place as many as possible.
- **Right click** an item to see a detailed layout with all of its recipes.
- **Middle click** an item to add it to bookmarks.

## Download

- [Modrinth](https://modrinth.com/mod/mod-recipe-book)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/mod-recipe-book)

## Requirements

NeoForge:

- NeoForge
- Architectury API
- JEI

Fabric:

- Fabric Loader
- Fabric API
- Architectury API
- JEI
- Mod Menu (Not required, but recommended, as it allows you to configure the categories and other mod options)

## Install

Put the matching jar in `mods` together with the loader, Architectury API, and JEI:

- `modrecipebook-xxx-x.x.x.jar`

## Build

```
./gradlew build
```

Run clients with `./gradlew :neoforge:runClient` or `./gradlew :fabric:runClient`.

## License

MIT
