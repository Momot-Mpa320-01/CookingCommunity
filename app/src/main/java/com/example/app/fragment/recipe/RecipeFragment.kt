package com.example.app.fragment.recipe

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.app.R
import com.example.app.model.RecipeModel
import com.example.app.model.UserModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_recipe.view.*
import java.util.*
import kotlin.collections.ArrayList

class RecipeFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val args by navArgs<RecipeFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_recipeFragment_to_navigation_home)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_recipe, container, false)
        val recipe = args.currentRecipe

        root.image_recipe_prev.setOnClickListener {
            findNavController().navigate(R.id.action_recipeFragment_to_navigation_home)
        }

//        reportRecipe(root, recipe)

        root.text_recipe_name.text = recipe.name

        loadImage(recipe.image, root.image_recipe)

        root.text_recipe_type.append(" " + resources.getStringArray(R.array.type_array)[recipe.type!!])

        root.text_recipe_diet.append(" " + resources.getStringArray(R.array.diet_array)[recipe.diet!!])

        loadIcons(root, recipe)

        loadOwner(recipe, root)

        loadUtensils(recipe, root)

        loadNbPeople(root, recipe)

        loadIngredients(recipe, root, recipe.ingredientsQuantity!!)

        loadSteps(recipe, root)

        clickableFavorite(root.button_recipe_fav, recipe.date!!, recipe.fav!!, root.text_recipe_fav)

        return root
    }

//    private fun reportRecipe(root: View, recipe: RecipeModel) {
//        root.button_report.setOnClickListener {
//            AlertDialog.Builder(context)
//                    .setTitle(getString(R.string.alert_report_title))
//                    .setMessage(getString(R.string.alert_report_message, recipe.name))
//                    .setPositiveButton(android.R.string.ok) { _, _ ->
//                        val send = Intent(Intent.ACTION_SENDTO)
//                        send.data = Uri.parse("mailto:xxx@gmail.com")
//                        send.putExtra(Intent.EXTRA_SUBJECT, "Signalement")
//                        send.putExtra(Intent.EXTRA_TEXT, "Nom = ${recipe.name}")
//                        startActivity(send)
//                    }
//                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
//                        dialog.cancel()
//                    }
//                    .show()
//        }
//    }

    private fun loadNbPeople(root: View, recipe: RecipeModel) {
        ArrayAdapter.createFromResource(
                root.context,
                R.array.nb_array,
                android.R.layout.simple_spinner_item
        ).also { arrayAdapter ->
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            root.spinner_nb.adapter = arrayAdapter
            root.spinner_nb.setSelection(
                    recipe.number!! - 1
            )
        }

        root.spinner_nb.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val factor = (position + 1).toFloat() / recipe.number!!.toFloat()
                val oldQuantity = recipe.ingredientsQuantity!!
                val newQuantity = ArrayList<String>(oldQuantity.size)

                for (ing in oldQuantity)
                    newQuantity.add(
                            "%.2f".format((ing.toLong() * factor))
                                    .replace(",00", "")
                                    .replace(".00", ""))
                loadIngredients(recipe, root, newQuantity)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadIngredients(recipe: RecipeModel, root: View, quantity: List<String>) {
        root.layout_ing.removeAllViews()
        for (i in quantity.indices) {
            val ingredientText = TextView(context)
            var type = ""
            if (recipe.ingredientsType!![i].toInt() in 1..4)
                type = resources.getStringArray(R.array.ingredient_type_array)[recipe.ingredientsType!![i].toInt()]
            val text = "- ${quantity[i]}$type ${recipe.ingredientsName!![i]}"
            ingredientText.text = text
            if (recipe.ingredientsType!![i].toInt() == 5) {
                val t = "- ${recipe.ingredientsName!![i]}"
                ingredientText.text = t
            }
            ingredientText.textSize = 16F
            root.layout_ing.addView(ingredientText)
        }
    }

    private fun loadUtensils(recipe: RecipeModel, root: View) {
        for (u in recipe.utensils!!.sorted()) {
            val utensilText = TextView(context)
            val utensil = resources.getStringArray(R.array.utensil_array)[u.toInt()]
            val text = "- $utensil"
            utensilText.text = text
            utensilText.textSize = 16F
            root.layout_utensils.addView(utensilText)
        }
    }

    private fun loadSteps(recipe: RecipeModel, root: View) {
        var count = 1
        for (steps in recipe.steps!!) {
            val stepText = TextView(context)
            val text = "$count) $steps\n"
            stepText.text = text
            stepText.textSize = 16F
            root.layout_steps.addView(stepText)
            count++
        }
    }

    private fun loadOwner(recipe: RecipeModel, root: View) {
        db.collection("users")
                .document(recipe.uid!!)
                .get().addOnSuccessListener { doc ->
                    val user = doc.toObject<UserModel>()!!
                    clickableOwner(root.text_recipe_owner, user, recipe.uid!!, recipe.date!!)
                }
    }

    private fun loadImage(imagePath: String?, imageView: ImageView) {
        if (imagePath != null) {
            val storageReference = FirebaseStorage.getInstance().reference.child(imagePath)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(requireContext())
                        .load(uri)
                        .into(imageView)
            }
        } else
            imageView.visibility = ImageView.GONE
    }

    private fun loadIcons(root: View, recipe: RecipeModel) {
        val pricesImages = listOf<ImageView>(
            root.image_recipe_price,
            root.image_recipe_price2,
            root.image_recipe_price3
        )
        val timesImages = listOf<ImageView>(
            root.image_recipe_time,
            root.image_recipe_time2,
            root.image_recipe_time3
        )

        for (i in 0..recipe.price!!)
            pricesImages[i].alpha = 1.0F
        for (i in 0..recipe.time!!)
            timesImages[i].alpha = 1.0F

        root.text_recipe_fav.text = recipe.fav.toString()
    }

    private fun clickableOwner(text: TextView, user: UserModel, uid: String, date: Date) {
        val name = user.name!!
        val clickableName = SpannableString(name)
        clickableName.setSpan(object : ClickableSpan() {
            override fun onClick(p0: View) {
                if (uid != FirebaseAuth.getInstance().currentUser!!.uid) {
                    val action = RecipeFragmentDirections.actionRecipeFragmentToAccountViewFragment(user, uid)
                    findNavController().navigate(action)
                } else
                    findNavController().navigate(R.id.action_recipeFragment_to_navigation_account)
            }
        }, 0, name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.append(" ")
        text.append(clickableName)
        text.append("\n" + getString(R.string.text_date, date.toLocaleString()))
        text.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun clickableFavorite(buttonRecipeFav: Button, date: Date, fav: Int, textFav: TextView) {
        if(currentUser!=null && !(currentUser.isAnonymous)){
            db.collection(getString(R.string.collection_recipes))
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { docs ->
                    for (doc in docs) {
                        db.collection(getString(R.string.collection_users))
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                val favList = document.get("favorites") as List<*>
                                if (favList.contains(doc.id)) {
                                    buttonRecipeFav.alpha = 0.1F
                                    buttonRecipeFav.isEnabled = false
                                } else {
                                    buttonRecipeFav.setOnClickListener {
                                        if (favList.size < 10)
                                            addToFavorite(buttonRecipeFav, doc, fav, currentUser, textFav)
                                        else {
                                            AlertDialog.Builder(requireContext())
                                                    .setTitle(getString(R.string.alert_impossible_title))
                                                    .setMessage(getString(R.string.alert_fav_message))
                                                    .setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
                                                    .show()
                                        }
                                        }
                                    }
                                }
                            }
                    }
                }
        else {
            buttonRecipeFav.setOnClickListener {
                Toast.makeText(context, getString(R.string.text_forbid_ano), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToFavorite(buttonRecipeFav: Button, doc: QueryDocumentSnapshot, fav: Int, currentUser: FirebaseUser, textFav: TextView) {
        db.collection(getString(R.string.collection_recipes))
                .document(doc.id)
                .update("fav", fav + 1)
        db.collection(getString(R.string.collection_users))
                .document(currentUser.uid)
                .update("favorites", FieldValue.arrayUnion(doc.id))
        AlertDialog.Builder(context)
                .setMessage(getString(R.string.dialog_fav))
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        textFav.text = (fav + 1).toString()
        buttonRecipeFav.alpha = 0.1F
        buttonRecipeFav.isEnabled = false
        return
    }
}