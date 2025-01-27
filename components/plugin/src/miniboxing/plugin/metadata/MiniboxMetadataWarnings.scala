//
//     _____   .__         .__ ___.                    .__ scala-miniboxing.org
//    /     \  |__|  ____  |__|\_ |__    ____  ___  ___|__|  ____     ____
//   /  \ /  \ |  | /    \ |  | | __ \  /  _ \ \  \/  /|  | /    \   / ___\
//  /    Y    \|  ||   |  \|  | | \_\ \(  <_> ) >    < |  ||   |  \ / /_/  >
//  \____|__  /|__||___|  /|__| |___  / \____/ /__/\_ \|__||___|  / \___  /
//          \/          \/          \/               \/         \/ /_____/
// Copyright (c) 2011-2015 Scala Team, École polytechnique fédérale de Lausanne
//
// Authors:
//    * Milos Stojanovic
//    * Vlad Ureche
//

package miniboxing.plugin
package metadata

trait MiniboxMetadataWarnings {
  self: MiniboxInjectComponent =>

  import global._
  import definitions._

  case class ForwardWarning(mboxedTypeParam: Symbol, nonMboxedType: Type, pos: Position) {
    def warn(warningType: ForwardWarningEnum.Value, inLibrary: Boolean): Unit = {
      if (metadata.miniboxedTParamFlag(mboxedTypeParam)) {
        val warning = warningType match {
          case ForwardWarningEnum.InnerClass =>
            new ForwardWarningForInnerClass(mboxedTypeParam, nonMboxedType, pos, inLibrary)
          case ForwardWarningEnum.StemClass =>
            new ForwardWarningForStemClass(mboxedTypeParam, nonMboxedType, pos, inLibrary)
          case ForwardWarningEnum.NotSpecificEnoughTypeParam =>
            new ForwardWarningForNotSpecificEnoughTypeParam(mboxedTypeParam, nonMboxedType, pos, inLibrary)
        }
        warning.warn()
      }
    }
  }

  object ForwardWarningEnum extends Enumeration {
    val InnerClass, StemClass, NotSpecificEnoughTypeParam = Value
  }

  case class BackwardWarning(nonMboxedTypeParam: Symbol, mboxedType: Type, pos: Position) {
    def warn(warningType: BackwardWarningEnum.Value, inLibrary: Boolean): Unit = {
      if (!metadata.miniboxedTParamFlag(nonMboxedTypeParam)) {
        val warning = warningType match {
          case BackwardWarningEnum.PrimitiveType =>
            new BackwardWarningForPrimitiveType(nonMboxedTypeParam, mboxedType, pos, inLibrary)
          case BackwardWarningEnum.MiniboxedTypeParam =>
            new BackwardWarningForMiniboxedTypeParam(nonMboxedTypeParam, mboxedType, pos, inLibrary)
        }
        warning.warn()
      }
    }
  }

  object BackwardWarningEnum extends Enumeration {
    val PrimitiveType, MiniboxedTypeParam = Value
  }

  abstract class MiniboxWarning(typeParam: Symbol, pos: Position, inLibrary: Boolean) {

    def msg(): String
    def shouldWarn(): Boolean

    def warn(): Unit =
      if (shouldWarn && !alreadyWarnedTypeParam && !alreadyWarnedPosition) {
        metadata.warningTypeParameters += typeParam
        metadata.warningPositions += pos
        suboptimalCodeWarning(pos, msg, typeParam.isGenericAnnotated, inLibrary)
      }

    lazy val alreadyWarnedTypeParam: Boolean =
      metadata.warningTypeParameters.contains(typeParam)

    lazy val alreadyWarnedPosition: Boolean =
      metadata.warningPositions.contains(pos)

    def isUselessWarning(p: Symbol): Boolean = {
      p.isMbArrayMethod ||
      p.isImplicitlyPredefMethod ||
      p.isCastSymbol ||
      p.isIsInstanceOfAnyMethod ||
      p.isArrowAssocMethod ||
      p.owner == Tuple1Class ||
      p.owner == Tuple2Class ||
      p.owner == FunctionClass(0) ||
      p.owner == FunctionClass(1) ||
      p.owner == FunctionClass(2)
    }

    // TODO: These guys should not be hijackers -- the control flow becomes too difficult to follow:
    def isOwnerArray(typeParam: Symbol, typeArg: Type, pos: Position): Boolean = {
      if (typeParam.owner.isArray) {
        (new UseMbArrayInsteadOfArrayWarning(typeParam, typeArg, pos)).warn()
        true
      } else false
    }

    // TODO: These guys should not be hijackers -- the control flow becomes too difficult to follow:
    def isSpecialized(typeParam: Symbol, pos: Position, inLibrary: Boolean): Boolean = {
      if (typeParam.hasAnnotation(SpecializedClass)) {
        (new ReplaceSpecializedWithMiniboxedWarning(typeParam, pos, inLibrary)).warn()
        true
      } else false
    }
  }

  class BackwardWarningForPrimitiveType(nonMboxedTypeParam: Symbol, mboxedType: Type, pos: Position, inLibrary: Boolean) extends MiniboxWarning(nonMboxedTypeParam, pos, inLibrary) {

    override def msg: String = s"The ${nonMboxedTypeParam.owner.tweakedFullString} would benefit from miniboxing type " +
                               s"parameter ${nonMboxedTypeParam.nameString}, since it is instantiated by a primitive type."

    override def shouldWarn(): Boolean = {
      !isUselessWarning(nonMboxedTypeParam.owner) &&
      !isOwnerArray(nonMboxedTypeParam, mboxedType, pos) &&
      !isSpecialized(nonMboxedTypeParam, pos, inLibrary)
    }
  }

  class BackwardWarningForMiniboxedTypeParam(nonMboxedTypeParam: Symbol, mboxedType: Type, pos: Position, inLibrary: Boolean) extends MiniboxWarning(nonMboxedTypeParam, pos, inLibrary) {

    override def msg: String = s"The ${nonMboxedTypeParam.owner.tweakedFullString} would benefit from miniboxing type " +
                               s"parameter ${nonMboxedTypeParam.nameString}, since it is instantiated by miniboxed " +
                               s"type parameter ${mboxedType.typeSymbol.nameString.stripSuffix("sp")} of " +
                               s"${metadata.getStem(mboxedType.typeSymbol.owner).tweakedToString}."

    override def shouldWarn(): Boolean = {
      !isUselessWarning(nonMboxedTypeParam.owner) &&
      !isOwnerArray(nonMboxedTypeParam, mboxedType, pos) &&
      !isSpecialized(nonMboxedTypeParam, pos, inLibrary)
    }
  }

  class ForwardWarningForStemClass(mboxedTypeParam: Symbol, nonMboxedType: Type, pos: Position, inLibrary: Boolean) extends MiniboxWarning(mboxedTypeParam, pos, inLibrary) {

    override def msg: String = "The following code could benefit from miniboxing specialization (the reason was explained before)."

    override def shouldWarn(): Boolean = {
      !isUselessWarning(mboxedTypeParam.owner)
    }
  }

  class ForwardWarningForInnerClass(mboxedTypeParam: Symbol, nonMboxedType: Type, pos: Position, inLibrary: Boolean) extends MiniboxWarning(mboxedTypeParam, pos, inLibrary) {

    override def msg: String = s"The following code could benefit from miniboxing specialization " +
                               s"if the type parameter ${nonMboxedType.typeSymbol.name} of ${nonMboxedType.typeSymbol.owner.tweakedToString} " +
                               s"""would be marked as "@miniboxed ${nonMboxedType.typeSymbol.name}" (it would be used to """ +
                               s"instantiate miniboxed type parameter ${mboxedTypeParam.name} of ${mboxedTypeParam.owner.tweakedToString})"

    override def shouldWarn(): Boolean = {
      !isUselessWarning(mboxedTypeParam.owner) &&
      !isSpecialized(mboxedTypeParam, pos, inLibrary)
    }
  }

  class ForwardWarningForNotSpecificEnoughTypeParam(mboxedTypeParam: Symbol, nonMboxedType: Type, pos: Position, inLibrary: Boolean = false) extends MiniboxWarning(mboxedTypeParam, pos, inLibrary) {

    override def msg: String = s"""Using the type argument "$nonMboxedType" for the miniboxed type parameter """ +
                               s"${mboxedTypeParam.name} of ${mboxedTypeParam.owner.tweakedToString} is not specific enough, " +
                               s"as it could mean either a primitive or a reference type. Although " +
                               s"${mboxedTypeParam.owner.tweakedToString} is miniboxed, it won't benefit from " +
                               s"specialization:"

    override def shouldWarn(): Boolean = {
      !isUselessWarning(mboxedTypeParam.owner) &&
      !isSpecialized(mboxedTypeParam, pos, inLibrary)
    }
  }

  class UseMbArrayInsteadOfArrayWarning(typeParam: Symbol, typeArg: Type, pos: Position, inLibrary: Boolean = false) extends MiniboxWarning(typeParam, pos, inLibrary) {

    override def msg: String = "Use MbArray instead of Array to eliminate the need for ClassTags and " +
                               "benefit from seamless interoperability with the miniboxing specialization. " +
                               "For more details about MbArrays, please check the following link: " +
                               "http://scala-miniboxing.org/arrays.html"

    // alternative: use the position
    override lazy val alreadyWarnedTypeParam = false

    override def shouldWarn(): Boolean = {
      flags.flag_warn_mbarrays &&
      ((typeParam.owner.isArray || (typeParam.owner == ArrayModule_genericApply)) &&
      typeArg.typeSymbol.deSkolemize.hasAnnotation(MinispecClass) || typeParam.owner.isClassTag)
    }
  }

  class ReplaceSpecializedWithMiniboxedWarning(p: Symbol, pos: Position, inLibrary: Boolean) extends MiniboxWarning(p, pos, inLibrary) {

    override def msg: String = s"Although the type parameter ${p.nameString} of ${p.owner.tweakedFullString} is " +
                                "specialized, miniboxing and specialization communicate among themselves by boxing " +
                                "(thus, inefficiently) on all classes other than as FunctionX and TupleX. If you " +
                                "want to maximize performance, consider switching from specialization to miniboxing: " +
                                "'@miniboxed T':"

    override def shouldWarn(): Boolean = {
      p.hasAnnotation(SpecializedClass)
    }
  }
}
