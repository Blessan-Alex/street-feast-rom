You’re getting this crash because your `ChefPageFragment` is trying to access its `ViewModel` **too early – while the fragment is still detached**.

Key part of the stacktrace:

```text
Caused by: java.lang.IllegalStateException: Can't access ViewModels from detached fragment
    ...
    at com.streatfeast.app.fragments.ChefPageFragment.getViewModel(ChefPageFragment.kt:48)
    at com.streatfeast.app.fragments.ChefPageFragment.<init>(ChefPageFragment.kt:78)
```

So:

* `ChefPageFragment` is being created during layout inflation (`FragmentContainerView` / navigation).
* During the **constructor** (or property initialization), something touches `viewModel`.
* `by viewModels()` needs the fragment to be attached to the activity, but at that moment it isn’t yet → crash.

---

## What’s almost certainly in your fragment

You likely have something like:

```kotlin
class ChefPageFragment : Fragment(R.layout.fragment_chef_page) {

    private val viewModel: ChefPageViewModel by viewModels()

    private val adapter = ChefAdapter(viewModel)  // ❌ this runs in constructor
}
```

or:

```kotlin
class ChefPageFragment : Fragment(R.layout.fragment_chef_page) {

    private val viewModel: ChefPageViewModel by viewModels()

    init {
        viewModel.loadStuff()   // ❌ using viewModel in init
    }
}
```

Anything that **uses** `viewModel` in:

* property initializers, or
* `init { }` block, or
* a custom constructor

will run *before* the fragment is attached.

---

## How to fix it

### 1. Keep `viewModel` declaration, but only use it after attachment

Keep this (it’s fine):

```kotlin
private val viewModel: ChefPageViewModel by viewModels()
```

But move **all uses** of `viewModel` into lifecycle methods like `onCreate`, `onViewCreated`, or later.

For example, instead of:

```kotlin
private val adapter = ChefAdapter(viewModel)  // ❌
```

do:

```kotlin
private lateinit var adapter: ChefAdapter

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    adapter = ChefAdapter(viewModel)   // ✅ safe now
    recyclerView.adapter = adapter

    viewModel.items.observe(viewLifecycleOwner) { items ->
        adapter.submitList(items)
    }
}
```

And remove any `init {}` block that touches `viewModel`; replace with `onCreate` or `onViewCreated`.

---

### 2. Make sure the fragment has a no-arg constructor

Your fragment class should **not** have any custom constructor parameters. It should look like:

```kotlin
class ChefPageFragment : Fragment(R.layout.fragment_chef_page) {
    // ...
}
```

If you need to pass data, use `arguments` / `Bundle` or the Safe Args plugin, not constructor params.

---

## Checklist for your file

Open `ChefPageFragment.kt` and check:

1. At **line 48** (from the stacktrace) – this is `getViewModel`:

   * Find `val viewModel` and see what code is using it.
2. At **line 78** – inside the constructor:

   * Look for `init {}`, property initializers that reference `viewModel`, or anything that uses it before `onCreate/onViewCreated`.

Refactor those usages into `onCreate` or `onViewCreated` as shown above.

---

If you paste your `ChefPageFragment` code, I can rewrite it into a safe version tailored exactly to your file.
