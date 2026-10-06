# The Spice Road - Spice Profiles
## Table of Contents
- [Introduction](#introduction)
- [Creating a Spice Profile](#creating-a-spice-profile)
- [Applying a Spice Profile](#applying-a-spice-profile)

<br>

## Introduction
A spice profile is a set of double-precision values that gets created on food items as the data component `spice_road:spice_profile`, which keeps track of the added spices and gives the items their unique effects when consumed.  
In the same way, it also describes the attributes the raw spice items will impart on the crafting outputs. In this case, the spice items don't have a data component, the profile instead needs to be supplied via a custom datapack JSON.  
An example profile could look like this:<!-- TODO: update -->  
```json
{
  "items": "spice_road:cinnamon",
  "profile": {
    "heat_cooling": 0.3,
    "sweet_bitter": 0.8,
    "sour_mellow": -0.3,
    "earthy_floral": 0.1,
    "woody_green": 0.9,
    "pungent_soft": 0.1,
    "resinous_clean": 0.2,
    "savory_delicate": -0.2
  }
}
```
Each axis is bipolar and has a key formatted as `<positive>_<negative>`. This means a spice cannot be heating and cooling at the same time, for example.  
Each time spices are mixed, these profile values get summed together, and taper off toward a maximum configurable upper range, usually `[-10.0, +10.0]`. Processing recipes like cooking items in furnaces will add random multipliers to the current spice profile, as the flavor compounds in real life also change depending on whether the spices were cooked or not.  
  
This document describes how to create a spice profile in the ["creating a spice profile" section](#creating-a-spice-profile), and how to apply it to food items in the ["applying a spice profile" section.](#applying-a-spice-profile)

<br>

## Creating a Spice Profile
TODO:

<br>

## Applying a Spice Profile
TODO:


<br><br><br><br>

<div style="text-align: center;" align="center">

---

[⬅️ Back: Tags](./tags.md) &bull; [🏠 Docs - Home](../README.md) &bull; [➡️ Next: Mix Presets](./mix_preset.md)

</div>
